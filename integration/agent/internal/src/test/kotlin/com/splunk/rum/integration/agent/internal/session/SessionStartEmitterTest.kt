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

package com.splunk.rum.integration.agent.internal.session

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.LOG_EVENT_NAME_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.RUM_TRACER_NAME
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SCREEN_NAME_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.RumConstants
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionStartEmitterTest {

    private val exporter = CollectingLogRecordExporter()
    private val loggerProvider = SdkLoggerProvider.builder()
        .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
        .build()

    private val emitter = SessionStartEmitter()

    @Before
    fun setUp() {
        emitter.attach(loggerProvider.get(RUM_TRACER_NAME))
    }

    @Test
    fun `holds the event while the session produces no other signal`() {
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)

        assertTrue(exporter.records.isEmpty())
    }

    @Test
    fun `emits the held event once the session produces a signal`() {
        emitter.onSessionCreated(
            "session-a",
            previousSessionId = "session-before",
            timestamp = 1_000L,
            screenName = "Home"
        )

        emitter.emitIfPending()

        val record = exporter.records.single()
        assertEquals(RumConstants.SESSION_START_EVENT_NAME, record.attributes.get(LOG_EVENT_NAME_KEY))
        assertEquals("session-a", record.attributes.get(SESSION_ID_KEY))
        assertEquals("session-before", record.attributes.get(PREVIOUS_SESSION_ID_KEY))
        assertEquals("Home", record.attributes.get(SCREEN_NAME_KEY))
    }

    @Test
    fun `emits the first session of a process with no previous session`() {
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)

        emitter.emitIfPending()

        val record = exporter.records.single()
        assertEquals("session-a", record.attributes.get(SESSION_ID_KEY))
        assertNull(record.attributes.get(PREVIOUS_SESSION_ID_KEY))
    }

    @Test
    fun `keeps the timestamp of the session rather than of the signal that released it`() {
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)

        emitter.emitIfPending()

        assertEquals(TimeUnit.MILLISECONDS.toNanos(1_000L), exporter.records.single().timestampEpochNanos)
    }

    @Test
    fun `emits the event only once no matter how many signals follow`() {
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)

        repeat(5) { emitter.emitIfPending() }

        assertEquals(1, exporter.records.size)
    }

    @Test
    fun `does not release a pending event for a different session`() {
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)

        emitter.emitIfPending("session-before")

        assertTrue(exporter.records.isEmpty())
    }

    @Test
    fun `releases a pending event when the signal belongs to that session`() {
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)

        emitter.emitIfPending("session-a")

        assertEquals("session-a", exporter.records.single().attributes.get(SESSION_ID_KEY))
    }

    @Test
    fun `drops the held event of a session that produced nothing before the next one started`() {
        emitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)
        emitter.onSessionCreated("session-b", previousSessionId = "session-a", timestamp = 2_000L)

        emitter.emitIfPending()

        assertEquals("session-b", exporter.records.single().attributes.get(SESSION_ID_KEY))
    }

    @Test
    fun `keeps the event held until a logger is available`() {
        val detachedEmitter = SessionStartEmitter()
        detachedEmitter.onSessionCreated("session-a", previousSessionId = null, timestamp = 1_000L)

        detachedEmitter.emitIfPending()
        assertTrue(exporter.records.isEmpty())

        detachedEmitter.attach(loggerProvider.get(RUM_TRACER_NAME))
        detachedEmitter.emitIfPending()

        assertEquals("session-a", exporter.records.single().attributes.get(SESSION_ID_KEY))
    }

    private class CollectingLogRecordExporter : LogRecordExporter {
        val records = CopyOnWriteArrayList<LogRecordData>()

        override fun export(logs: Collection<LogRecordData>): CompletableResultCode {
            records += logs
            return CompletableResultCode.ofSuccess()
        }

        override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

        override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
    }
}
