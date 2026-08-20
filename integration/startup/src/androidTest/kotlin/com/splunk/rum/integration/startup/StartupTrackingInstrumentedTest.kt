/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.startup

import android.app.Application
import android.app.Instrumentation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.startup.ApplicationStartupTimekeeper
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for startup tracking.
 *
 * Startup callbacks are seeded directly because AndroidJUnitRunner starts the target process in the
 * background, which causes its first test activity to be classified as a warm start. This mirrors
 * the direct detector seeding in NavigationTrackingInstrumentedTest while exercising the real
 * startup listener, deferred cache, module installation, OpenTelemetry SDK, and span exporter.
 *
 * Cold and hot starts are intentionally exercised in a single test because startup tracking
 * maintains process-wide state.
 */
@RunWith(AndroidJUnit4::class)
class StartupTrackingInstrumentedTest {

    private val exportedSpans = CopyOnWriteArrayList<SpanData>()

    private val collectingExporter = object : SpanExporter {
        override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
            exportedSpans.addAll(spans)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()
        override fun shutdown() = CompletableResultCode.ofSuccess()
    }

    private val instrumentation: Instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private lateinit var tracerProvider: SdkTracerProvider

    @Before
    fun setUp() {
        exportedSpans.clear()
        tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(collectingExporter))
            .build()
        SplunkOpenTelemetrySdk.instance = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build()
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
        tracerProvider.shutdown().join(5, TimeUnit.SECONDS)
    }

    @Test
    fun deferredColdStartAndSubsequentHotStartEmitAppStartSpans() {
        reportColdStart()

        assertTrue("Cold start should be deferred until agent installation", appStartSpans().isEmpty())

        val application = instrumentation.targetContext.applicationContext as Application
        AgentIntegration.instance.install(
            application,
            SplunkOpenTelemetrySdk.instance!!,
            listOf(StartupModuleConfiguration()),
            Attributes.empty()
        )

        waitForIdle()

        val coldStart = appStartSpans().single()
        assertEquals(RumConstants.APP_START_TYPE_COLD, coldStart.startType())

        reportHotStart()
        waitForIdle()

        val appStarts = appStartSpans()
        assertEquals("Expected cold and hot AppStart spans", 2, appStarts.size)
        assertEquals(
            listOf(RumConstants.APP_START_TYPE_COLD, RumConstants.APP_START_TYPE_HOT),
            appStarts.map { it.startType() }
        )

        val initializationSpans = exportedSpans.filter {
            it.name == RumConstants.APP_START_INITIALIZE_SPAN_NAME
        }
        assertEquals("Initialization should only be reported once", 1, initializationSpans.size)
        assertEquals(coldStart.spanId, initializationSpans.single().parentSpanId)
    }

    private fun reportColdStart() {
        val endTimestamp = System.currentTimeMillis()
        val startTimestamp = endTimestamp - START_DURATION_MILLIS
        ApplicationStartupTimekeeper.listeners.forEach {
            it.onColdStarted(startTimestamp, endTimestamp, START_DURATION_MILLIS)
        }
    }

    private fun reportHotStart() {
        val endTimestamp = System.currentTimeMillis()
        val startTimestamp = endTimestamp - START_DURATION_MILLIS
        ApplicationStartupTimekeeper.listeners.forEach {
            it.onHotStarted(startTimestamp, endTimestamp, START_DURATION_MILLIS)
        }
    }

    private fun waitForIdle() {
        instrumentation.waitForIdleSync()
        val latch = CountDownLatch(1)
        instrumentation.runOnMainSync { latch.countDown() }
        assertTrue("Main thread did not become idle", latch.await(5, TimeUnit.SECONDS))
    }

    private fun appStartSpans(): List<SpanData> = exportedSpans.filter {
        it.name == RumConstants.APP_START_SPAN_NAME
    }

    private fun SpanData.startType(): String? = attributes.get(RumConstants.APP_START_TYPE_KEY)

    private companion object {
        const val START_DURATION_MILLIS = 100L
    }
}
