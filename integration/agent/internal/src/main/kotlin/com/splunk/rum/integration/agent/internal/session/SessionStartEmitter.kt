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

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.LOG_EVENT_NAME_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.RumConstants
import io.opentelemetry.api.logs.Logger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Holds the `session.start` event of a newly created session until that session produces its first
 * other signal.
 *
 * A process that never produces telemetry, such as a headless start from a job, receiver or push
 * handler, therefore exports nothing at all and no session materializes for it.
 */
internal class SessionStartEmitter {

    private val pending = AtomicReference<PendingSessionStart?>()

    @Volatile
    private var logger: Logger? = null

    fun attach(logger: Logger) {
        this.logger = logger
    }

    /**
     * Holds the `session.start` event of [sessionId]. An event still held for an earlier session is
     * dropped, because that session produced no telemetry to own it.
     */
    fun onSessionCreated(sessionId: String, previousSessionId: String?, timestamp: Long) {
        val event = PendingSessionStart(sessionId, previousSessionId, timestamp)
        pending.set(event)
    }

    /**
     * Emits the held event, if there is one, with the timestamp of the session it belongs to.
     *
     * Clearing the slot before emitting keeps the emitted record from re-entering here through the
     * processors that call this and emitting the event twice.
     */
    fun emitIfPending() {
        if (logger == null) return
        val event = pending.getAndSet(null) ?: return

        emit(event)
    }

    private fun emit(event: PendingSessionStart) {
        val logger = logger ?: return

        logger.logRecordBuilder()
            .setAttribute(LOG_EVENT_NAME_KEY, RumConstants.SESSION_START_EVENT_NAME)
            .setTimestamp(event.timestamp, TimeUnit.MILLISECONDS)
            .setAttribute(SESSION_ID_KEY, event.sessionId)
            .apply { event.previousSessionId?.let { setAttribute(PREVIOUS_SESSION_ID_KEY, it) } }
            .emit()
    }

    private class PendingSessionStart(val sessionId: String, val previousSessionId: String?, val timestamp: Long)
}
