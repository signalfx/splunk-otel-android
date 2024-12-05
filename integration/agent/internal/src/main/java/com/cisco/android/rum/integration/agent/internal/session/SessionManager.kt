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

package com.cisco.android.rum.integration.agent.internal.session

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.smartlook.sdk.common.id.NanoId
import com.smartlook.sdk.common.storage.preferences.IPreferences
import com.smartlook.sdk.common.utils.AppStateObserver
import com.smartlook.sdk.common.utils.extensions.forEachFast
import com.smartlook.sdk.common.utils.extensions.safeSchedule
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture

class SessionManager internal constructor(
    private val preferences: IPreferences
) {

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val pulseHandler: Handler
    private val appStateObserver = AppStateObserver()

    private var sessionValidityWatcher: ScheduledFuture<*>? = null

    val anonId: String
        get() = preferences.getString(ANON_ID_KEY) ?: throw IllegalStateException("Should never be null")

    /**
     * The value is valid after the [install] function is called.
     */
    @set:Synchronized
    var sessionId: String
        get() = createNewSessionIfNeeded()
        private set(value) {
            preferences
                .putString(SESSION_ID_KEY, value)
                .putLong(SESSION_VALID_UNTIL_KEY, System.currentTimeMillis() + maxSessionLength)
                .apply()
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
        if (ANON_ID_KEY !in preferences)
            preferences
                .putString(ANON_ID_KEY, NanoId.generate())
                .apply()
    }

    @Synchronized
    private fun createNewSessionIfNeeded(): String {
        val savedSessionId = preferences.getString(SESSION_ID_KEY)
        val sessionValidInBackgroundUntil = preferences.getLong(SESSION_VALID_IN_BACKGROUND_UNTIL_KEY)
        val sessionValidUntil = preferences.getLong(SESSION_VALID_UNTIL_KEY)
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
        preferences
            .putLong(SESSION_VALID_IN_BACKGROUND_UNTIL_KEY, System.currentTimeMillis() + sessionTimeout)
            .apply()
    }

    private fun deleteSessionInBackgroundValidationTime() {
        preferences
            .remove(SESSION_VALID_IN_BACKGROUND_UNTIL_KEY)
            .apply()
    }

    private fun deleteSessionValidationTime() {
        preferences
            .remove(SESSION_VALID_UNTIL_KEY)
            .apply()
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

        const val ANON_ID_KEY = "anonId"
        const val SESSION_ID_KEY = "sessionId"
        const val SESSION_VALID_UNTIL_KEY = "sessionValidUntil"
        const val SESSION_VALID_IN_BACKGROUND_UNTIL_KEY = "sessionValidInBackgroundUntil"
    }
}
