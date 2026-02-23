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
import androidx.annotation.VisibleForTesting
import com.splunk.android.common.utils.AppStateObserver
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.android.common.utils.extensions.safeSchedule
import com.splunk.rum.common.storage.IAgentStorage
import com.splunk.rum.common.storage.SessionId as SessionIdStorageData
import com.splunk.rum.integration.agent.internal.id.SessionId
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager.SessionListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture

interface ISplunkSessionManager {
    val sessionId: String
    val sessionStart: Long
    val sessionLastActivity: Long
    val previousSessionId: String?
    val sessionListeners: MutableSet<SessionListener>

    fun install(context: Context)
    fun trackSessionActivity()
    fun reset()
    fun sessionId(timestamp: Long): String
}

object NoOpSplunkSessionManager : ISplunkSessionManager {
    override val sessionId: String = ""
    override val sessionStart: Long = 0
    override val sessionLastActivity: Long = 0
    override val previousSessionId: String? = null
    override val sessionListeners: MutableSet<SessionListener> = mutableSetOf()
    override fun install(context: Context) = Unit
    override fun trackSessionActivity() = Unit
    override fun reset() = Unit

    override fun sessionId(timestamp: Long): String = ""
}

class SplunkSessionManager internal constructor(private val agentStorage: IAgentStorage) : ISplunkSessionManager {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val appStateObserver = AppStateObserver()

    private var sessionValidityWatcher: ScheduledFuture<*>? = null

    private val sessionIds: MutableList<SessionIdStorageData> by lazy {
        agentStorage.readSessionIds().toMutableList()
    }

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

    @get:Synchronized
    override val sessionStart: Long
        get() {
            val sessionId = sessionId
            return sessionIds.lastOrNull { it.id == sessionId }?.validFrom ?: System.currentTimeMillis()
        }

    @get:Synchronized
    override val sessionLastActivity: Long
        get() = agentStorage.readSessionLastActivity() ?: sessionStart

    override var previousSessionId: String? = null
        private set

    override val sessionListeners: MutableSet<SessionListener> = HashSet()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var sessionBackgroundTimeout: Long = DEFAULT_SESSION_BACKGROUND_TIMEOUT

    private var maxSessionLength: Long = DEFAULT_SESSION_LENGTH

    override fun install(context: Context) {
        createNewSessionIfNeeded()

        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    override fun reset() {
        clearLastSession()
    }

    override fun sessionId(timestamp: Long): String = sessionIds
        .filter { it.validFrom <= timestamp }
        .maxByOrNull { it.validFrom }
        ?.id
        ?: throw IllegalArgumentException("No valid session for timestamp: $timestamp")

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
        deleteSessionLastActivity()

        val newSessionId = SessionId.generate()
        sessionId = newSessionId
        sessionIds.add(SessionIdStorageData(newSessionId, now))
        agentStorage.writeSessionIds(sessionIds)
        sessionListeners.forEachFast { it.onSessionChanged(newSessionId, now) }
        return newSessionId
    }

    override fun trackSessionActivity() {
        agentStorage.writeSessionLastActivity(System.currentTimeMillis())
    }

    fun deleteSessionLastActivity() {
        agentStorage.deleteSessionLastActivity()
    }

    private fun clearLastSession() {
        deleteSessionValidationTime()
        deleteSessionInBackgroundValidationTime()
        deleteSessionLastActivity()
        agentStorage.deleteSessionId()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun watchSessionInBackgroundValidity() {
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
        fun onSessionChanged(sessionId: String, timestamp: Long)
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
