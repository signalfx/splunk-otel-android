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

package com.splunk.rum.integration.agent.internal.processor

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.RumConstants
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.InstrumentationScopeInfo
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.logs.data.LogRecordData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Answers.RETURNS_SELF
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SessionReplaySessionIdLogProcessorTest {

    private val sessionManager = mock(ISplunkSessionManager::class.java)

    @Test
    fun `passes the timestamp-resolved session id to the signal callback`() {
        val logRecord = mock(ReadWriteLogRecord::class.java, RETURNS_SELF)
        val logRecordData = mock(LogRecordData::class.java)
        `when`(logRecordData.instrumentationScopeInfo).thenReturn(
            InstrumentationScopeInfo.builder(
                GlobalRumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME
            ).build()
        )
        `when`(logRecordData.timestampEpochNanos).thenReturn(1_000_000_000L)
        `when`(logRecord.toLogRecordData()).thenReturn(logRecordData)
        `when`(sessionManager.sessionId(1_000L)).thenReturn("session-a")
        var signaledSessionId: String? = null
        val onSessionSignal: (String) -> Unit = { signaledSessionId = it }
        val processor = SessionReplaySessionIdLogProcessor(sessionManager, onSessionSignal)

        processor.onEmit(Context.root(), logRecord)

        verify(logRecord).setAttribute(GlobalRumConstants.SESSION_ID_KEY, "session-a")
        verify(logRecord).setAttribute(RumConstants.SESSION_RUM_ID_KEY, "session-a")
        assertEquals("session-a", signaledSessionId)
    }
}
