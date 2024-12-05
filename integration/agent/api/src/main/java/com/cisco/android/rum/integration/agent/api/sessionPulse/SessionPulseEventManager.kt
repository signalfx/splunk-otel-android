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

package com.cisco.android.rum.integration.agent.api.sessionPulse

import com.cisco.mrum.common.otel.api.OpenTelemetry
import com.cisco.android.rum.integration.agent.api.attributes.AttributeConstants
import com.cisco.android.rum.integration.agent.internal.session.SessionManager
import com.smartlook.sdk.common.logger.Logger
import com.smartlook.sdk.log.LogAspect
import io.opentelemetry.api.common.Attributes
import java.util.concurrent.TimeUnit

internal class SessionPulseEventManager(sessionManager: SessionManager) {

    init {
        Logger.privateD(LogAspect.SDK_METHODS, TAG, { "init()" })

        sessionManager.pulseListeners += object : SessionManager.PulseListener {
            override fun onPulseEvent() {
                createSessionPulseEvent(sessionManager.sessionId)
            }
        }
    }

    private fun createSessionPulseEvent(sessionId: String) {
        Logger.privateD(LogAspect.SDK_METHODS, TAG, { "createSessionPulseEvent() sessionId: $sessionId" })

        val instance = OpenTelemetry.instance ?: return

        val now = System.currentTimeMillis()
        val attributes = Attributes.of(
            AttributeConstants.NAME, "session_pulse",
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
        fun obtainInstance(sessionManager: SessionManager): SessionPulseEventManager {
            if (instanceInternal == null)
                instanceInternal = SessionPulseEventManager(sessionManager)

            return requireNotNull(instanceInternal)
        }
    }
}
