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

package com.splunk.rum.integration.agent.internal.session

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.DEFAULT_SCREEN_NAME
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.LOG_EVENT_NAME_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SCREEN_NAME_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.common.logger.Logger as InternalLogger
import com.splunk.rum.integration.agent.internal.RumConstants
import io.opentelemetry.api.logs.Logger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds `session.start` until emission. With `allowBackgroundSessionCreation=true`, it waits for
 * accepted telemetry. With `false`, it is emitted when the session is created.
 */
internal class SessionStartEmitter {

    private val pending = AtomicReference<PendingSessionStart?>()

    @Volatile
    private var logger: Logger? = null

    fun attach(logger: Logger) {
        this.logger = logger
    }

    /** Replaces any pending `session.start` from an earlier session. */
    fun onSessionCreated(
        sessionId: String,
        previousSessionId: String?,
        timestamp: Long,
        screenName: String = DEFAULT_SCREEN_NAME
    ) {
        val event = PendingSessionStart(sessionId, previousSessionId, timestamp, screenName)
        pending.set(event)
    }

    /**
     * Emits the matching event. Clears it first to prevent reentry from emitting it twice.
     * A failed emit is not retried because it may already be queued.
     */
    fun emitIfPending(sessionId: String? = null) {
        if (logger == null) return

        while (true) {
            val event = pending.get() ?: return
            if (sessionId != null && event.sessionId != sessionId) return
            if (pending.compareAndSet(event, null)) {
                try {
                    emit(event)
                } catch (e: Exception) {
                    runCatching { InternalLogger.e(TAG, "Failed to emit session.start", e) }
                }
                return
            }
        }
    }

    private fun emit(event: PendingSessionStart) {
        val logger = logger ?: return

        logger.logRecordBuilder()
            .setAttribute(LOG_EVENT_NAME_KEY, RumConstants.SESSION_START_EVENT_NAME)
            .setTimestamp(event.timestamp, TimeUnit.MILLISECONDS)
            .setAttribute(SESSION_ID_KEY, event.sessionId)
            .setAttribute(SCREEN_NAME_KEY, event.screenName)
            .apply { event.previousSessionId?.let { setAttribute(PREVIOUS_SESSION_ID_KEY, it) } }
            .emit()
    }

    private class PendingSessionStart(
        val sessionId: String,
        val previousSessionId: String?,
        val timestamp: Long,
        val screenName: String
    )

    private companion object {
        const val TAG = "SessionStartEmitter"
    }
}
