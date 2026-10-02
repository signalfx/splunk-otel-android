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
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

class SessionActivityLogProcessor(private val sessionManager: ISplunkSessionManager) : LogRecordProcessor {
    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        // session.start records when the session began, but may be processed later after the first
        // real signal. That signal already updated lastActivity, so session.start should not move
        // it forward based on its later processing time.
        if (logRecord.toLogRecordData().attributes.get(LOG_EVENT_NAME_KEY) !=
            RumConstants.SESSION_START_EVENT_NAME
        ) {
            sessionManager.trackSessionActivity()
        }
    }
}
