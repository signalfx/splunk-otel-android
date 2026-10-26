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
import com.splunk.rum.agent.common.otel.internal.withAppInstallationId
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.common.job.IJobManager
import com.splunk.rum.common.job.JobIdStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.resources.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AndroidLogRecordExporterTest {

    private var storedPayload = ByteArray(0)
    private val storage = mock(IAgentStorage::class.java).also {
        `when`(it.readOrCreateAppInstallationId()).thenReturn("installation-id")
        doAnswer { invocation ->
            storedPayload = invocation.getArgument(1)
            true
        }.`when`(it).writeOtelSessionReplayData(anyString(), anyByteArray())
    }

    @Test
    fun `session replay data is stored with the app installation id`() {
        val exporter = AndroidLogRecordExporter(storage, mock(IJobManager::class.java), mock(JobIdStorage::class.java))

        exporter.export(mutableListOf(sessionReplayLog()))

        val payload = String(storedPayload, Charsets.ISO_8859_1)
        assertTrue(payload.contains("installation-id"))
        assertFalse(payload.contains(IAgentStorage.PENDING_APP_INSTALLATION_ID))
    }

    @Test
    fun `log with the app installation id keeps all other data`() {
        val log = sessionReplayLog()

        val completed = log.withAppInstallationId(storage)

        assertEquals("installation-id", completed.resource.getAttribute(APP_INSTALLATION_ID_KEY))
        assertEquals(log.instrumentationScopeInfo, completed.instrumentationScopeInfo)
        assertEquals(log.timestampEpochNanos, completed.timestampEpochNanos)
        assertEquals(log.observedTimestampEpochNanos, completed.observedTimestampEpochNanos)
        assertEquals(log.spanContext, completed.spanContext)
        assertEquals(log.severity, completed.severity)
        assertEquals(log.severityText, completed.severityText)
        assertEquals(log.bodyValue, completed.bodyValue)
        assertEquals(log.attributes, completed.attributes)
        assertEquals(log.totalAttributeCount, completed.totalAttributeCount)
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
            .setResource(
                Resource.create(Attributes.of(APP_INSTALLATION_ID_KEY, IAgentStorage.PENDING_APP_INSTALLATION_ID))
            )
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

    private companion object {
        val APP_INSTALLATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("app.installation.id")
    }
}
