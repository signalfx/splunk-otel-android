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

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.common.job.IJobManager
import com.splunk.rum.common.job.JobIdStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

class AndroidLogRecordExporterTest {

    private var storedPayload = ByteArray(0)
    private val storage = mock(IAgentStorage::class.java).also {
        doAnswer { invocation ->
            storedPayload = invocation.getArgument(1)
            true
        }.`when`(it).writeOtelSessionReplayData(anyString(), anyByteArray())
    }

    @Test
    fun `session replay data is stored unchanged`() {
        val exporter = AndroidLogRecordExporter(storage, mock(IJobManager::class.java), mock(JobIdStorage::class.java))
        val log = sessionReplayLog()

        exporter.export(mutableListOf(log))

        val expectedPayload = ByteArrayOutputStream().use {
            LogsRequestMarshaler.create(listOf(log)).writeBinaryTo(it)
            it.toByteArray()
        }
        assertArrayEquals(expectedPayload, storedPayload)
    }

    private fun sessionReplayLog(): LogRecordData {
        val captured = mutableListOf<LogRecordData>()
        val exporter = object : LogRecordExporter {
            override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
                captured += logs
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }

        SdkLoggerProvider.builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
            .build()
            .get(GlobalRumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME)
            .logRecordBuilder()
            .setBody("recording")
            .setSeverity(Severity.INFO)
            .setSeverityText("INFO")
            .setAttribute(AttributeKey.stringKey("event.name"), "session_replay_data")
            .emit()

        return captured.single()
    }

    private fun anyByteArray(): ByteArray {
        any(ByteArray::class.java)
        return ByteArray(0)
    }
}
