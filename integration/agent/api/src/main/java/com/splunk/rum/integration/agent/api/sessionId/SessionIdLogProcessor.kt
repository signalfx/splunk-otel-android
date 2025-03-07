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

package com.splunk.rum.integration.agent.api.sessionId

import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.api.attributes.AttributeConstants.SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

internal class SessionIdLogProcessor(private val sessionManager: SplunkSessionManager) : LogRecordProcessor {
    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        val sessionId = sessionManager.sessionId
        Logger.d("SessionIdLogProcessor", "onEmit sessionId: $sessionId, ${logRecord.toLogRecordData().attributes}")
        logRecord.setAttribute(SESSION_ID_KEY, sessionId)
    }
}
