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

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class SessionIdSpanProcessorTest {

    private val sessionManager = mock(ISplunkSessionManager::class.java)
    private val processor = SessionIdSpanProcessor(sessionManager)

    @Test
    fun `preserves a previous session id already attached to the span`() {
        val span = mockSpan(
            Attributes.of(
                SESSION_ID_KEY,
                "session-a",
                PREVIOUS_SESSION_ID_KEY,
                "session-before"
            )
        )
        `when`(sessionManager.previousSessionId).thenReturn("session-a")

        processor.onStart(Context.root(), span)

        verify(span, never()).setAttribute(PREVIOUS_SESSION_ID_KEY, "session-a")
    }

    @Test
    fun `adds the current previous session id when the span has none`() {
        val span = mockSpan(Attributes.of(SESSION_ID_KEY, "session-a"))
        `when`(sessionManager.previousSessionId).thenReturn("session-before")

        processor.onStart(Context.root(), span)

        verify(span).setAttribute(PREVIOUS_SESSION_ID_KEY, "session-before")
    }

    private fun mockSpan(attributes: Attributes): ReadWriteSpan {
        val span = mock(ReadWriteSpan::class.java)
        `when`(span.attributes).thenReturn(attributes)
        return span
    }
}
