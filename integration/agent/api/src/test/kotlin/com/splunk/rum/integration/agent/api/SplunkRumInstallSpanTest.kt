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

package com.splunk.rum.integration.agent.api

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanId
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SplunkRumInstallSpanTest {

    private val exportedSpans = CopyOnWriteArrayList<SpanData>()

    private val collectingExporter = object : SpanExporter {
        override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
            exportedSpans.addAll(spans)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush() = CompletableResultCode.ofSuccess()
        override fun shutdown() = CompletableResultCode.ofSuccess()
    }

    private lateinit var tracerProvider: SdkTracerProvider
    private lateinit var openTelemetry: OpenTelemetrySdk

    @Before
    fun setUp() {
        exportedSpans.clear()
        tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(collectingExporter))
            .build()
        openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build()
    }

    @After
    fun tearDown() {
        tracerProvider.shutdown().join(5, TimeUnit.SECONDS)
    }

    @Test
    fun `emitInstallSpan creates span with correct name`() {
        val startMillis = System.currentTimeMillis() - 100
        val startElapsedNanos = SystemClock.elapsedRealtimeNanos() - TimeUnit.MILLISECONDS.toNanos(100)

        SplunkRum.emitInstallSpan(openTelemetry, startMillis, startElapsedNanos)

        assertEquals(1, exportedSpans.size)
        assertEquals("SplunkRum.install", exportedSpans[0].name)
    }

    @Test
    fun `emitInstallSpan creates root span with no parent`() {
        val startMillis = System.currentTimeMillis() - 50
        val startElapsedNanos = SystemClock.elapsedRealtimeNanos() - TimeUnit.MILLISECONDS.toNanos(50)

        SplunkRum.emitInstallSpan(openTelemetry, startMillis, startElapsedNanos)

        val span = exportedSpans.single()
        assertEquals(SpanId.getInvalid(), span.parentSpanId)
    }

    @Test
    fun `emitInstallSpan uses provided start timestamp`() {
        val startMillis = System.currentTimeMillis() - 200
        val startElapsedNanos = SystemClock.elapsedRealtimeNanos() - TimeUnit.MILLISECONDS.toNanos(200)

        SplunkRum.emitInstallSpan(openTelemetry, startMillis, startElapsedNanos)

        val span = exportedSpans.single()
        val spanStartMillis = TimeUnit.NANOSECONDS.toMillis(span.startEpochNanos)
        assertEquals(startMillis, spanStartMillis)
    }

    @Test
    fun `emitInstallSpan end timestamp is after start timestamp`() {
        val startMillis = System.currentTimeMillis() - 100
        val startElapsedNanos = SystemClock.elapsedRealtimeNanos() - TimeUnit.MILLISECONDS.toNanos(100)

        SplunkRum.emitInstallSpan(openTelemetry, startMillis, startElapsedNanos)

        val span = exportedSpans.single()
        assertTrue(
            "End epoch (${span.endEpochNanos}) should be > start epoch (${span.startEpochNanos})",
            span.endEpochNanos > span.startEpochNanos
        )
    }

    @Test
    fun `emitInstallSpan does not throw with noop OpenTelemetry`() {
        SplunkRum.emitInstallSpan(
            OpenTelemetry.noop(),
            System.currentTimeMillis(),
            SystemClock.elapsedRealtimeNanos()
        )
    }
}
