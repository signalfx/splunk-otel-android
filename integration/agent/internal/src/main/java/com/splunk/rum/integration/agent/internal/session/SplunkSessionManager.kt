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

package com.splunk.rum.integration.agent.internal.session

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import com.cisco.android.common.id.NanoId
import com.cisco.android.common.utils.AppStateObserver
import com.cisco.android.common.utils.extensions.forEachFast
import com.cisco.android.common.utils.extensions.safeSchedule
import com.splunk.sdk.common.storage.IAgentStorage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture

class SplunkSessionManager internal constructor(
    private val agentStorage: IAgentStorage
) {

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val pulseHandler: Handler
    private val appStateObserver = AppStateObserver()

    private var sessionValidityWatcher: ScheduledFuture<*>? = null

    val anonId: String
        get() = agentStorage.readAnonId() ?: throw IllegalStateException("Should never be null")

    /**
     * The value is valid after the [install] function is called.
     */
    @set:Synchronized
    var sessionId: String
        get() = createNewSessionIfNeeded()
        private set(value) {
            agentStorage.writeSessionId(value)
            agentStorage.writeSessionValidUntil(System.currentTimeMillis() + maxSessionLength)
        }

    var sessionTimeout: Long = DEFAULT_SESSION_TIMEOUT

    var maxSessionLength: Long = DEFAULT_SESSION_LENGTH

    val sessionListeners: MutableSet<SessionListener> = HashSet()

    val pulseListeners: MutableSet<PulseListener> = HashSet()

    init {
        val pulseThread = HandlerThread("PulseThread")
        pulseThread.start()
        pulseHandler = Handler(pulseThread.looper)

        createAnonIdIfNeeded()
    }

    fun install(context: Context) {
        clearLastSession()
        createNewSessionIfNeeded()

        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    private fun createAnonIdIfNeeded() {
        if (agentStorage.readAnonId() == null) {
            agentStorage.writeAnonId(NanoId.generate())
        }
    }

    @Synchronized
    private fun createNewSessionIfNeeded(): String {
        val savedSessionId = agentStorage.readSessionId()
        val sessionValidInBackgroundUntil = agentStorage.readSessionValidUntilInBackground()
        val sessionValidUntil = agentStorage.readSessionValidUntil()
        val now = System.currentTimeMillis()

        val backgroundValidity = if (sessionValidInBackgroundUntil != null) sessionValidInBackgroundUntil > now else true

        val isCurrentSessionIdValid = savedSessionId != null &&
                backgroundValidity &&
                sessionValidUntil != null &&
                sessionValidUntil > now

        if (isCurrentSessionIdValid) {
            return requireNotNull(savedSessionId)
        }

        deleteSessionInBackgroundValidationTime()
        deleteSessionValidationTime()

        val newSessionId = NanoId.generate()
        sessionId = newSessionId
        sessionListeners.forEachFast { it.onSessionChanged(newSessionId) }
        return newSessionId
    }

    private fun clearLastSession() {
        deleteSessionValidationTime()
        deleteSessionInBackgroundValidationTime()
    }

    private fun watchSessionInBackgroundValidity() {
        saveSessionInBackgroundValidationTime()

        sessionValidityWatcher = executor.safeSchedule(sessionTimeout) {
            sessionValidityWatcher = null

            deleteSessionInBackgroundValidationTime()
            deleteSessionValidationTime()

            createNewSessionIfNeeded()
        }
    }

    private fun cancelSessionWatcher() {
        sessionValidityWatcher?.cancel(false)
        sessionValidityWatcher = null
    }

    private fun saveSessionInBackgroundValidationTime() {
        agentStorage.writeSessionValidUntilInBackground(System.currentTimeMillis() + sessionTimeout)
    }

    private fun deleteSessionInBackgroundValidationTime() {
        agentStorage.deleteSessionValidUntilInBackground()
    }

    private fun deleteSessionValidationTime() {
        agentStorage.deleteSessionValidUntil()
    }

    private fun schedulePulseEvent() {
        pulseHandler.removeCallbacks(triggerAndSchedulePulseEvent)
        pulseHandler.postDelayed(triggerAndSchedulePulseEvent, DEFAULT_PULSE_EVENT_LENGTH)
    }

    private fun unSchedulePulseEvent() {
        pulseHandler.removeCallbacks(triggerAndSchedulePulseEvent)
    }

    private val triggerAndSchedulePulseEvent = Runnable {
        triggerPulseEvent()
        schedulePulseEvent()
    }

    private fun triggerPulseEvent() {
        pulseListeners.forEachFast { it.onPulseEvent() }
    }

    interface SessionListener {
        fun onSessionChanged(sessionId: String)
    }

    interface PulseListener {
        fun onPulseEvent()
    }

    private inner class AppStateObserverListener : AppStateObserver.Listener {

        override fun onAppStarted() {
            deleteSessionInBackgroundValidationTime()
        }

        override fun onAppBackgrounded() {
            watchSessionInBackgroundValidity()
            unSchedulePulseEvent()
            triggerPulseEvent()
        }

        override fun onAppForegrounded() {
            triggerAndSchedulePulseEvent.run()
            cancelSessionWatcher()
        }

        override fun onAppClosed() {
            cancelSessionWatcher()
            saveSessionInBackgroundValidationTime()
            unSchedulePulseEvent()
        }
    }

    private companion object {
        const val DEFAULT_SESSION_TIMEOUT = 900_000L
        const val DEFAULT_SESSION_LENGTH = 60L * 60L * 1000L
        const val DEFAULT_PULSE_EVENT_LENGTH = 5L * 60L * 1000L // 5 minutes
    }
}
