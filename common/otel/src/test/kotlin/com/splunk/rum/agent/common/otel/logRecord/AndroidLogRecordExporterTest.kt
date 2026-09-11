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

import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.common.job.IJobManager
import com.splunk.rum.common.job.JobIdStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Value
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.stubbing.Answer

class AndroidLogRecordExporterTest {
    private var storedSessionReplayId: String? = null
    private var bufferedSessionReplayId: String? = null
    private var storedSessionReplayPayload: ByteArray? = null
    private val agentStorage = mock(
        IAgentStorage::class.java,
        Answer { invocation ->
            when (invocation.method.name) {
                "writeOtelSessionReplayData" -> {
                    storedSessionReplayId = invocation.getArgument(0)
                    storedSessionReplayPayload = invocation.getArgument(1)
                    true
                }
                "addBufferedSessionReplayId" -> {
                    bufferedSessionReplayId = invocation.getArgument(0)
                    null
                }
                else -> RETURNS_DEFAULTS.answer(invocation)
            }
        }
    )
    private val jobManager = mock(IJobManager::class.java)
    private val jobIdStorage = mock(JobIdStorage::class.java)
    private val exportedSpans = mutableListOf<SpanData>()
    private lateinit var tracerProvider: SdkTracerProvider
    private lateinit var exporter: AndroidLogRecordExporter

    @Before
    fun setUp() {
        val spanExporter = object : SpanExporter {
            override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
                exportedSpans += spans
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }
        tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build()
        SplunkOpenTelemetrySdk.instance = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build()
        exporter = AndroidLogRecordExporter(agentStorage, jobManager, jobIdStorage)
    }

    @After
    fun tearDown() {
        SplunkOpenTelemetrySdk.instance = null
        tracerProvider.shutdown().join(10, TimeUnit.SECONDS)
    }

    @Test
    fun `converts a general log using the stable event name API`() {
        val log = createLog("general-scope") {
            setTimestamp(123_456L, TimeUnit.NANOSECONDS)
            setEventName("stable.event")
            setBody("log body")
            setAttribute(AttributeKey.stringKey("empty.value"), "")
        }

        exporter.export(mutableListOf(log))

        val span = exportedSpans.single()
        assertEquals("stable.event", span.name)
        assertEquals(span.startEpochNanos, span.endEpochNanos)
        assertEquals("", span.attributes.get(AttributeKey.stringKey("empty.value")))
    }

    @Test
    fun `falls back to the default event name attribute for existing producers`() {
        val log = createLog("general-scope") {}

        exporter.export(mutableListOf(log))

        assertEquals("splunk.log", exportedSpans.single().name)
    }

    @Test
    fun `writes Session Replay as OTLP logs with an explicitly present empty string`() {
        val replayBody = byteArrayOf(1, 2, 3, 4)
        val emptyAttributeKey = "session.replay.empty"
        val log = createLog(GlobalRumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME) {
            setTimestamp(987_654L, TimeUnit.NANOSECONDS)
            setBody(Value.of(replayBody))
            setAttribute(AttributeKey.stringKey(emptyAttributeKey), "")
        }

        exporter.export(mutableListOf(log))

        assertEquals(storedSessionReplayId, bufferedSessionReplayId)
        val payload = checkNotNull(storedSessionReplayPayload)
        assertTrue(payload.isNotEmpty())
        assertTrue(
            payload.containsSubsequence(
                GlobalRumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME.toByteArray(StandardCharsets.UTF_8)
            )
        )
        assertTrue(payload.containsSubsequence(encodedEmptyStringAttribute(emptyAttributeKey)))
        assertTrue(payload.containsSubsequence(replayBody))
    }

    private fun createLog(scopeName: String, configure: LogRecordBuilder.() -> Unit): LogRecordData {
        val capturedLogs = mutableListOf<LogRecordData>()
        val logExporter = object : LogRecordExporter {
            override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
                capturedLogs += logs
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }
        val loggerProvider = SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
            .build()
        try {
            loggerProvider.get(scopeName).logRecordBuilder().apply(configure).emit()
            return capturedLogs.single()
        } finally {
            loggerProvider.shutdown().join(10, TimeUnit.SECONDS)
        }
    }

    private fun encodedEmptyStringAttribute(key: String): ByteArray {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        require(keyBytes.size < 128)
        return byteArrayOf(0x0a, keyBytes.size.toByte()) + keyBytes + byteArrayOf(0x12, 0x02, 0x0a, 0x00)
    }

    private fun ByteArray.containsSubsequence(expected: ByteArray): Boolean {
        if (expected.isEmpty()) {
            return true
        }
        return indices.any { start ->
            start + expected.size <= size && expected.indices.all { offset -> this[start + offset] == expected[offset] }
        }
    }
}
