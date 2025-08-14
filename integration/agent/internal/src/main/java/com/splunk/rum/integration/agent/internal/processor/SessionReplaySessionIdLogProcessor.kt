/*
 * Copyright 2025 Splunk Inc.
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

import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.internal.attributes.AttributeConstants.SCRIPT_INSTANCE_KEY
import com.splunk.rum.integration.agent.internal.attributes.AttributeConstants.SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.attributes.AttributeConstants.SESSION_RUM_ID_KEY
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

class SessionReplaySessionIdLogProcessor(private val sessionManager: ISplunkSessionManager) : LogRecordProcessor {
    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        val logRecordData = logRecord.toLogRecordData()
        if (logRecordData.instrumentationScopeInfo.name == RumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME) {
            logRecord.setAttribute(SESSION_ID_KEY, sessionManager.sessionId(logRecordData.timestampEpochNanos))
                .setAttribute(SESSION_RUM_ID_KEY, sessionManager.sessionId(logRecordData.timestampEpochNanos))
                .setAttribute(SCRIPT_INSTANCE_KEY, sessionManager.scriptId())
        }
    }
}
