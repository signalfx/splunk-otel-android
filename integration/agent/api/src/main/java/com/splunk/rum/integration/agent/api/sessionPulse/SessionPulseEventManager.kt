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

package com.splunk.rum.integration.agent.api.sessionPulse

import com.cisco.android.common.logger.Logger
import com.splunk.sdk.common.otel.SplunkRumOpenTelemetrySdk
import com.splunk.rum.integration.agent.api.attributes.AttributeConstants.NAME
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.TimeUnit

internal class SessionPulseEventManager(sessionManager: SplunkSessionManager) {

    init {
        Logger.d(TAG, "init()")

        sessionManager.pulseListeners += object : SplunkSessionManager.PulseListener {
            override fun onPulseEvent() {
                createSessionPulseEvent(sessionManager.sessionId)
            }
        }
    }

    private fun createSessionPulseEvent(sessionId: String) {
        Logger.d(TAG, "createSessionPulseEvent() sessionId: $sessionId")

        val instance = SplunkRumOpenTelemetrySdk.instance ?: return

        val now = System.currentTimeMillis()
        val attributes = Attributes.of(
            NAME, "session_pulse",
        )
        // TODO Scope
        instance.sdkLoggerProvider
            .loggerBuilder("SessionPulseEvent")
            .build()
            .logRecordBuilder()
            .setAllAttributes(attributes)
            .setTimestamp(now, TimeUnit.MILLISECONDS)
            .setObservedTimestamp(now, TimeUnit.MILLISECONDS)
            .emit()
    }

    companion object {
        private const val TAG = "SessionPulseEventManager"
        private var instanceInternal: SessionPulseEventManager? = null
        fun obtainInstance(sessionManager: SplunkSessionManager): SessionPulseEventManager {
            if (instanceInternal == null)
                instanceInternal = SessionPulseEventManager(sessionManager)

            return requireNotNull(instanceInternal)
        }
    }
}
