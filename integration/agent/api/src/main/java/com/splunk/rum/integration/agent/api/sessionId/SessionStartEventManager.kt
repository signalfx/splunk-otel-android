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
import com.splunk.android.rum.integration.agent.api.attributes.AttributeConstants
import com.splunk.rum.integration.agent.internal.session.SessionManager
import com.cisco.mrum.common.otel.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.TimeUnit

internal class SessionStartEventManager(sessionManager: SessionManager) {

    init {
        Logger.d(com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager.Companion.TAG, "init()")

        sessionManager.sessionListeners += object : SessionManager.SessionListener {
            override fun onSessionChanged(sessionId: String) {
                createSessionStartEvent(sessionId, sessionManager.anonId)
            }
        }
    }

    private fun createSessionStartEvent(sessionId: String, userId: String) {
        Logger.d(com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager.Companion.TAG, "createSessionStartEvent() sessionId: $sessionId, userId: $userId")

        val instance = OpenTelemetry.instance ?: return

        val now = System.currentTimeMillis()
        val attributes = Attributes.of(
            com.splunk.android.rum.integration.agent.api.attributes.AttributeConstants.NAME, "session_start",
            AttributeKey.stringKey("enduser.anon_id"), userId,
        )
        // TODO Scope
        instance.sdkLoggerProvider
            .loggerBuilder("SessionStartEventScopeName")
            .build()
            .logRecordBuilder()
            .setAllAttributes(attributes)
            .setTimestamp(now, TimeUnit.MILLISECONDS)
            .emit()
    }

    companion object {
        private const val TAG = "SessionStartEventManager"
        private var instanceInternal: com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager? = null
        fun obtainInstance(sessionManager: SessionManager): com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager {
            if (com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager.Companion.instanceInternal == null)
                com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager.Companion.instanceInternal =
                    com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager(sessionManager)

            return requireNotNull(com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager.Companion.instanceInternal)
        }
    }
}
