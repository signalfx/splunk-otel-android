/*
 * Copyright 2024 Splunk Inc.
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

import com.splunk.rum.integration.agent.internal.attributes.AttributeConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.attributes.AttributeConstants.SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class SessionIdSpanProcessor(private val sessionManager: ISplunkSessionManager) : SpanProcessor {
    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        span.setAttribute(SESSION_ID_KEY, sessionManager.sessionId)
        span.setAttribute(PREVIOUS_SESSION_ID_KEY, sessionManager.previousSessionId)
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) = Unit

    override fun isEndRequired(): Boolean = false
}
