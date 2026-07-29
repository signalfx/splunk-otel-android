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

package com.splunk.rum.instrumentation.anr.internal

import com.splunk.rum.instrumentation.anr.internal.extractor.AnrAttributesExtractor
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.semconv.ExceptionAttributes
import java.util.Collections
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnrReporterTest {

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

    @Before
    fun setUp() {
        exportedSpans.clear()
        val tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(collectingExporter))
            .build()
        sdk = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()
    }

    @After
    fun tearDown() {
        sdk.close()
    }

    @Test
    fun `emits an error span with the expected scope, name and stack trace`() {
        val reporter = AnrReporter(sdk, emptyList())

        reporter.report(stackTrace())

        val span = exportedSpans.single()
        assertEquals(AnrReporter.ANR_SPAN_NAME, span.name)
        assertEquals(AnrReporter.ANR_INSTRUMENTATION_SCOPE_NAME, span.instrumentationScopeInfo.name)
        assertEquals(StatusCode.ERROR, span.status.statusCode)

        val stacktrace = span.attributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE)
        assertNotNull(stacktrace)
        assertTrue(stacktrace!!.contains("a.b.Class.foo"))
        assertTrue(stacktrace.contains("a.b.AnotherClass.bar"))
    }

    @Test
    fun `applies attributes from every extractor`() {
        val reporter = AnrReporter(
            sdk,
            listOf(
                AnrAttributesExtractor { attributes, _ -> attributes.put(AttributeKey.stringKey("first"), "1") },
                AnrAttributesExtractor { attributes, _ -> attributes.put(AttributeKey.stringKey("second"), "2") }
            )
        )

        reporter.report(stackTrace())

        val span = exportedSpans.single()
        assertEquals("1", span.attributes.get(AttributeKey.stringKey("first")))
        assertEquals("2", span.attributes.get(AttributeKey.stringKey("second")))
    }

    @Test
    fun `exposes the main thread stack trace to extractors`() {
        val trace = stackTrace()
        var captured: Array<StackTraceElement>? = null

        val reporter = AnrReporter(
            sdk,
            listOf(AnrAttributesExtractor { _, stackTrace -> captured = stackTrace })
        )

        reporter.report(trace)

        assertTrue(trace.contentEquals(captured))
    }

    @Test
    fun `a failing extractor does not prevent the ANR from being reported`() {
        val reporter = AnrReporter(
            sdk,
            listOf(
                AnrAttributesExtractor { _, _ -> throw IllegalArgumentException("extractor failed") },
                AnrAttributesExtractor { attributes, _ -> attributes.put(AttributeKey.stringKey("survivor"), "yes") }
            )
        )

        reporter.report(stackTrace())

        val span = exportedSpans.single()
        assertEquals(AnrReporter.ANR_SPAN_NAME, span.name)
        assertEquals("yes", span.attributes.get(AttributeKey.stringKey("survivor")))
    }

    private fun stackTrace(): Array<StackTraceElement> = arrayOf(
        StackTraceElement("a.b.Class", "foo", "Class.java", 42),
        StackTraceElement("a.b.AnotherClass", "bar", "AnotherClass.java", 123)
    )
}
