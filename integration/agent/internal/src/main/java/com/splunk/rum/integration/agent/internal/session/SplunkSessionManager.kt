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
import com.cisco.android.common.utils.AppStateObserver
import com.cisco.android.common.utils.extensions.forEachFast
import com.cisco.android.common.utils.extensions.safeSchedule
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager.SessionListener
import com.splunk.rum.integration.agent.internal.utils.TraceId
import com.splunk.sdk.common.storage.IAgentStorage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture

interface ISplunkSessionManager {
    val sessionId: String
    val previousSessionId: String?
    val sessionListeners: MutableSet<SessionListener>

    fun install(context: Context)
}

object NoOpSplunkSessionManager : ISplunkSessionManager {
    override val sessionId: String = ""
    override val previousSessionId: String? = null
    override val sessionListeners: MutableSet<SessionListener> = mutableSetOf()
    override fun install(context: Context) = Unit
}

class SplunkSessionManager internal constructor(private val agentStorage: IAgentStorage) : ISplunkSessionManager {

    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val appStateObserver = AppStateObserver()

    private var sessionValidityWatcher: ScheduledFuture<*>? = null

    /**
     * The value is valid after the [install] function is called.
     */
    @set:Synchronized
    override var sessionId: String
        get() = createNewSessionIfNeeded()
        private set(value) {
            previousSessionId = agentStorage.readSessionId()

            agentStorage.writeSessionId(value)
            agentStorage.writeSessionValidUntil(System.currentTimeMillis() + maxSessionLength)
        }

    override var previousSessionId: String?
        get() = agentStorage.readPreviousSessionId()
        private set(value) = agentStorage.writePreviousSessionId(value)

    override val sessionListeners: MutableSet<SessionListener> = HashSet()

    private var sessionBackgroundTimeout: Long = DEFAULT_SESSION_BACKGROUND_TIMEOUT

    private var maxSessionLength: Long = DEFAULT_SESSION_LENGTH

    override fun install(context: Context) {
        clearLastSession()
        createNewSessionIfNeeded()

        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    @Synchronized
    private fun createNewSessionIfNeeded(): String {
        val savedSessionId = agentStorage.readSessionId()
        val sessionValidInBackgroundUntil = agentStorage.readSessionValidUntilInBackground()
        val sessionValidUntil = agentStorage.readSessionValidUntil()
        val now = System.currentTimeMillis()

        val backgroundValidity = if (sessionValidInBackgroundUntil !=
            null
        ) {
            sessionValidInBackgroundUntil > now
        } else {
            true
        }

        val isCurrentSessionIdValid = savedSessionId != null &&
            backgroundValidity &&
            sessionValidUntil != null &&
            sessionValidUntil > now

        if (isCurrentSessionIdValid) {
            return requireNotNull(savedSessionId)
        }

        deleteSessionInBackgroundValidationTime()
        deleteSessionValidationTime()

        val newSessionId = TraceId.random()
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

        sessionValidityWatcher = executor.safeSchedule(sessionBackgroundTimeout) {
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
        agentStorage.writeSessionValidUntilInBackground(System.currentTimeMillis() + sessionBackgroundTimeout)
    }

    private fun deleteSessionInBackgroundValidationTime() {
        agentStorage.deleteSessionValidUntilInBackground()
    }

    private fun deleteSessionValidationTime() {
        agentStorage.deleteSessionValidUntil()
    }

    interface SessionListener {
        fun onSessionChanged(sessionId: String)
    }

    private inner class AppStateObserverListener : AppStateObserver.Listener {

        override fun onAppStarted() {
            deleteSessionInBackgroundValidationTime()
        }

        override fun onAppBackgrounded() {
            watchSessionInBackgroundValidity()
        }

        override fun onAppForegrounded() {
            cancelSessionWatcher()
        }

        override fun onAppClosed() {
            cancelSessionWatcher()
            saveSessionInBackgroundValidationTime()
        }
    }

    private companion object {
        const val DEFAULT_SESSION_BACKGROUND_TIMEOUT = 15L * 60L * 1000L // 15m
        const val DEFAULT_SESSION_LENGTH = 4L * 60L * 60L * 1000L // 4h
    }
}
