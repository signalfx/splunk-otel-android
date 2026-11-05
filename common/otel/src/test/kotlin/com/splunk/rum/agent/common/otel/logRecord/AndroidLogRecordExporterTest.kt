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

package com.splunk.rum.agent.common.otel.logRecord

import com.splunk.rum.agent.common.otel.RumConstants
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.common.job.IJobManager
import com.splunk.rum.common.job.JobIdStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class AndroidLogRecordExporterTest {

    private lateinit var sdk: OpenTelemetrySdk
    private var previousSdk: OpenTelemetrySdk? = null

    @Before
    fun setUp() {
        previousSdk = SplunkOpenTelemetrySdk.instance
        sdk = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build()
        SplunkOpenTelemetrySdk.instance = sdk
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = previousSdk
        sdk.shutdown()
    }

    @Test
    fun `crash payload contains attributes added by the span interceptor`() {
        val storage = mock(IAgentStorage::class.java)
        var storedPayload: ByteArray? = null
        doAnswer { invocation ->
            storedPayload = invocation.getArgument(1)
            true
        }.`when`(storage).writeOtelSpanData(anyString(), anyByteArray())
        val interceptor: (SpanData) -> SpanData = { span ->
            object : DelegatingSpanData(span) {
                override fun getAttributes(): Attributes = span.attributes.toBuilder()
                    .put(AttributeKey.stringKey("crash.interceptor.test"), "interceptor-applied")
                    .build()
            }
        }
        val exporter = AndroidLogRecordExporter(
            storage,
            mock(IJobManager::class.java),
            mock(JobIdStorage::class.java),
            interceptor
        )

        exporter.export(mutableListOf(crashLog()))

        val serialized = String(checkNotNull(storedPayload), Charsets.ISO_8859_1)
        assertTrue(serialized.contains("crash.interceptor.test"))
        assertTrue(serialized.contains("interceptor-applied"))
    }

    @Test
    fun `crash payload is not stored when the span interceptor rejects it`() {
        val storage = mock(IAgentStorage::class.java)
        val exporter = AndroidLogRecordExporter(
            storage,
            mock(IJobManager::class.java),
            mock(JobIdStorage::class.java)
        ) { null }

        exporter.export(mutableListOf(crashLog()))

        verify(storage, never()).writeOtelSpanData(anyString(), anyByteArray())
        verify(storage, never()).addBufferedSpanId(anyString())
    }

    @Test
    fun `crash interceptor throwables skip direct persistence`() {
        for (failure in listOf(IllegalStateException("interceptor failed"), AssertionError("interceptor failed"))) {
            val storage = mock(IAgentStorage::class.java)
            val exporter = AndroidLogRecordExporter(
                storage,
                mock(IJobManager::class.java),
                mock(JobIdStorage::class.java)
            ) { throw failure }

            exporter.export(mutableListOf(crashLog()))

            verify(storage, never()).writeOtelSpanData(anyString(), anyByteArray())
            verify(storage, never()).addBufferedSpanId(anyString())
        }
    }

    @Test
    fun `crash interceptor failure does not prevent storing the next crash record`() {
        val storage = mock(IAgentStorage::class.java)
        doAnswer { true }.`when`(storage).writeOtelSpanData(anyString(), anyByteArray())
        var interceptedCount = 0
        val exporter = AndroidLogRecordExporter(
            storage,
            mock(IJobManager::class.java),
            mock(JobIdStorage::class.java)
        ) { span ->
            interceptedCount++
            if (interceptedCount == 1) throw AssertionError("interceptor failed")
            span
        }

        exporter.export(mutableListOf(crashLog(), crashLog()))

        assertEquals(2, interceptedCount)
        verify(storage, times(1)).writeOtelSpanData(anyString(), anyByteArray())
        verify(storage, times(1)).addBufferedSpanId(anyString())
    }

    private fun crashLog(): LogRecordData {
        val captured = mutableListOf<LogRecordData>()
        val captureExporter = object : LogRecordExporter {
            override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
                captured.addAll(logs)
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }
        val provider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(captureExporter))
            .build()
        provider.get(RumConstants.CRASH_INSTRUMENTATION_SCOPE_NAME)
            .logRecordBuilder()
            .setAttribute(AttributeKey.stringKey("event.name"), "device.crash")
            .emit()
        provider.shutdown()
        return captured.single()
    }

    private fun anyByteArray(): ByteArray {
        any(ByteArray::class.java)
        return ByteArray(0)
    }
}
