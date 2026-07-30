/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum.instrumentation.slowrendering.internal

import android.app.Activity
import android.content.ComponentName
import android.os.Build
import android.os.Handler
import android.view.FrameMetrics
import android.view.Window
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.time.Duration
import java.util.Collections
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class SlowRenderListenerTest {

    private val exportedSpans: MutableList<SpanData> = Collections.synchronizedList(mutableListOf())

    private val collectingExporter = object : SpanExporter {
        override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
            exportedSpans.addAll(spans)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()
        override fun shutdown() = CompletableResultCode.ofSuccess()
    }

    private lateinit var sdk: OpenTelemetrySdk
    private lateinit var tracer: Tracer
    private lateinit var executor: ScheduledExecutorService
    private lateinit var frameMetricsHandler: Handler

    @Before
    fun setUp() {
        exportedSpans.clear()
        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(collectingExporter))
            .build()
        sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()
        tracer = sdk.getTracer(SlowRenderingInstrumentation.INSTRUMENTATION_SCOPE_NAME)
        executor = mock(ScheduledExecutorService::class.java)
        frameMetricsHandler = mock(Handler::class.java)
    }

    @After
    fun tearDown() {
        sdk.close()
    }

    @Test
    fun `start schedules polling at the configured interval`() {
        val listener = newListener(Duration.ofSeconds(2))

        listener.start()

        verify(executor).scheduleWithFixedDelay(any(), eq(2000L), eq(2000L), eq(TimeUnit.MILLISECONDS))
    }

    @Test
    fun `onActivityResumed registers a single frame metrics listener on the frame metrics thread`() {
        val listener = newListener()
        val window = mock(Window::class.java)
        val activity = activity(window)

        listener.onActivityResumed(activity)
        listener.onActivityResumed(activity)

        verify(window, times(1)).addOnFrameMetricsAvailableListener(any(), eq(frameMetricsHandler))
    }

    @Test
    fun `onActivityPaused unregisters the frame metrics listener`() {
        val listener = newListener()
        val window = mock(Window::class.java)
        val activity = activity(window)

        listener.onActivityResumed(activity)
        listener.onActivityPaused(activity)

        verify(window).removeOnFrameMetricsAvailableListener(any())
    }

    @Test
    fun `aggregates and classifies slow and frozen frames into spans on pause`() {
        val listener = newListener()
        val window = mock(Window::class.java)
        val activity = activity(window)

        val perActivityListener = resumeAndCapture(listener, window, activity)

        // 2 slow, 1 frozen, 1 normal (ignored).
        perActivityListener.onFrameMetricsAvailable(window, frame(20), 0)
        perActivityListener.onFrameMetricsAvailable(window, frame(30), 0)
        perActivityListener.onFrameMetricsAvailable(window, frame(800), 0)
        perActivityListener.onFrameMetricsAvailable(window, frame(5), 0)

        listener.onActivityPaused(activity)

        val expectedName = ComponentName(PACKAGE, CLASS).flattenToShortString()

        val slow = exportedSpans.single { it.name == SlowRenderListener.SLOW_RENDERS_SPAN_NAME }
        assertEquals(2L, slow.attributes.get(AttributeKey.longKey("count")))
        assertEquals(expectedName, slow.attributes.get(AttributeKey.stringKey("activity.name")))

        val frozen = exportedSpans.single { it.name == SlowRenderListener.FROZEN_RENDERS_SPAN_NAME }
        assertEquals(1L, frozen.attributes.get(AttributeKey.longKey("count")))
        assertEquals(expectedName, frozen.attributes.get(AttributeKey.stringKey("activity.name")))
    }

    @Test
    fun `ignores the first draw frame and negative durations`() {
        val listener = newListener()
        val window = mock(Window::class.java)
        val activity = activity(window)

        val perActivityListener = resumeAndCapture(listener, window, activity)

        // First draw frame -> ignored despite huge duration.
        perActivityListener.onFrameMetricsAvailable(window, frame(5000, firstDrawFrame = 1), 0)
        // Negative duration -> ignored.
        perActivityListener.onFrameMetricsAvailable(window, frameWithRawDuration(-1L), 0)

        listener.onActivityPaused(activity)

        assertTrue(exportedSpans.isEmpty())
    }

    @Test
    fun `does not emit spans when only fast frames are recorded`() {
        val listener = newListener()
        val window = mock(Window::class.java)
        val activity = activity(window)

        val perActivityListener = resumeAndCapture(listener, window, activity)

        perActivityListener.onFrameMetricsAvailable(window, frame(1), 0)
        perActivityListener.onFrameMetricsAvailable(window, frame(16), 0) // at threshold, not > 16

        listener.onActivityPaused(activity)

        assertTrue(exportedSpans.isEmpty())
    }

    @Test
    fun `scheduled poll drains per-activity histograms`() {
        val listener = newListener()
        val window = mock(Window::class.java)
        val activity = activity(window)

        listener.start()
        val pollTask = captureScheduledTask()

        val perActivityListener = resumeAndCapture(listener, window, activity)
        perActivityListener.onFrameMetricsAvailable(window, frame(800), 0)

        pollTask.run()

        val frozen = exportedSpans.single { it.name == SlowRenderListener.FROZEN_RENDERS_SPAN_NAME }
        assertEquals(1L, frozen.attributes.get(AttributeKey.longKey("count")))

        // Histogram resets after reporting: a second poll with no new frames emits nothing.
        exportedSpans.clear()
        pollTask.run()
        assertTrue(exportedSpans.isEmpty())
    }

    @Test
    fun `pausing an activity that was never resumed does nothing`() {
        val listener = newListener()
        val window = mock(Window::class.java)
        val activity = activity(window)

        listener.onActivityPaused(activity)

        verify(window, never()).removeOnFrameMetricsAvailableListener(any())
        assertTrue(exportedSpans.isEmpty())
        assertNull(exportedSpans.firstOrNull())
    }

    private fun newListener(pollInterval: Duration = Duration.ofSeconds(1)) =
        SlowRenderListener(tracer, executor, frameMetricsHandler, pollInterval)

    private fun activity(window: Window): Activity {
        val activity = mock(Activity::class.java)
        `when`(activity.window).thenReturn(window)
        `when`(activity.componentName).thenReturn(ComponentName(PACKAGE, CLASS))
        return activity
    }

    private fun resumeAndCapture(
        listener: SlowRenderListener,
        window: Window,
        activity: Activity
    ): Window.OnFrameMetricsAvailableListener {
        listener.onActivityResumed(activity)
        val captor = ArgumentCaptor.forClass(Window.OnFrameMetricsAvailableListener::class.java)
        verify(window).addOnFrameMetricsAvailableListener(captor.capture(), any())
        return captor.value
    }

    private fun captureScheduledTask(): Runnable {
        val captor = ArgumentCaptor.forClass(Runnable::class.java)
        verify(executor).scheduleWithFixedDelay(captor.capture(), any(Long::class.java), any(Long::class.java), any())
        return captor.value
    }

    private fun frame(durationMs: Long, firstDrawFrame: Long = 0): FrameMetrics =
        frameWithRawDuration(TimeUnit.MILLISECONDS.toNanos(durationMs), firstDrawFrame)

    private fun frameWithRawDuration(drawDurationNs: Long, firstDrawFrame: Long = 0): FrameMetrics {
        val frameMetrics = mock(FrameMetrics::class.java)
        `when`(frameMetrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME)).thenReturn(firstDrawFrame)
        `when`(frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)).thenReturn(drawDurationNs)
        return frameMetrics
    }

    private companion object {
        private const val PACKAGE = "com.splunk.rum.sample"
        private const val CLASS = "com.splunk.rum.sample.MainActivity"
    }
}
