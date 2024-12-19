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

package com.cisco.android.rum.integration.agent.api.sessionId

import com.cisco.android.common.logger.Logger
import com.cisco.android.rum.integration.agent.api.attributes.AttributeConstants
import com.cisco.android.rum.integration.agent.internal.session.SessionManager
import com.cisco.mrum.common.otel.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.TimeUnit

internal class SessionStartEventManager(sessionManager: SessionManager) {

    init {
        Logger.d(TAG, "init()")

        sessionManager.sessionListeners += object : SessionManager.SessionListener {
            override fun onSessionChanged(sessionId: String) {
                createSessionStartEvent(sessionId, sessionManager.anonId)
            }
        }
    }

    private fun createSessionStartEvent(sessionId: String, userId: String) {
        Logger.d(TAG, "createSessionStartEvent() sessionId: $sessionId, userId: $userId")

        val instance = OpenTelemetry.instance ?: return

        val now = System.currentTimeMillis()
        val attributes = Attributes.of(
            AttributeConstants.NAME, "session_start",
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
        private var instanceInternal: SessionStartEventManager? = null
        fun obtainInstance(sessionManager: SessionManager): SessionStartEventManager {
            if (instanceInternal == null)
                instanceInternal = SessionStartEventManager(sessionManager)

            return requireNotNull(instanceInternal)
        }
    }
}
