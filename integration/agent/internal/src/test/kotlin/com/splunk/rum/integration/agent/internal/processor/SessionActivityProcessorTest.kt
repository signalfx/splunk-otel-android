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

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.LOG_EVENT_NAME_KEY
import com.splunk.rum.integration.agent.internal.RumConstants
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SessionActivityProcessorTest {

    private val sessionManager = mock(ISplunkSessionManager::class.java)

    @Test
    fun `session start log does not update session activity`() {
        val processor = SessionActivityLogProcessor(sessionManager)
        val logRecord = mockLogRecord(
            Attributes.of(LOG_EVENT_NAME_KEY, RumConstants.SESSION_START_EVENT_NAME)
        )

        processor.onEmit(Context.root(), logRecord)

        verify(sessionManager, never()).trackSessionActivity()
    }

    @Test
    fun `ordinary log updates session activity`() {
        val processor = SessionActivityLogProcessor(sessionManager)
        val logRecord = mockLogRecord(Attributes.empty())

        processor.onEmit(Context.root(), logRecord)

        verify(sessionManager).trackSessionActivity()
    }

    @Test
    fun `session start span does not update session activity`() {
        val processor = SessionActivitySpanProcessor(sessionManager)
        val span = mockSpan(RumConstants.SESSION_START_EVENT_NAME)

        processor.onStart(Context.root(), span)

        verify(sessionManager, never()).trackSessionActivity()
    }

    @Test
    fun `ordinary span updates session activity`() {
        val processor = SessionActivitySpanProcessor(sessionManager)
        val span = mockSpan("http.request")

        processor.onStart(Context.root(), span)

        verify(sessionManager).trackSessionActivity()
    }

    private fun mockLogRecord(attributes: Attributes): ReadWriteLogRecord {
        val logRecord = mock(ReadWriteLogRecord::class.java)
        val logRecordData = mock(LogRecordData::class.java)
        `when`(logRecord.toLogRecordData()).thenReturn(logRecordData)
        `when`(logRecordData.attributes).thenReturn(attributes)
        return logRecord
    }

    private fun mockSpan(name: String): ReadWriteSpan {
        val span = mock(ReadWriteSpan::class.java)
        `when`(span.name).thenReturn(name)
        return span
    }
}
