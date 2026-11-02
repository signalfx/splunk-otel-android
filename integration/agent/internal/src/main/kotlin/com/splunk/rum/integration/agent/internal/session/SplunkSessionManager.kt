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
import androidx.annotation.VisibleForTesting
import com.splunk.rum.agent.common.storage.IAgentStorage
import com.splunk.rum.agent.common.storage.SessionId as SessionIdStorageData
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.common.utils.extensions.forEachFast
import com.splunk.rum.common.utils.extensions.safeSchedule
import com.splunk.rum.integration.agent.internal.id.SessionId
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager.SessionListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture

interface ISplunkSessionManager {
    val sessionId: String?
    val sessionStart: Long?
    val sessionLastActivity: Long?
    val sessionSnapshot: SessionSnapshot?
    val previousSessionId: String?
    val sessionListeners: MutableSet<SessionListener>

    fun install(application: Application)
    fun trackSessionActivity()
    fun reset()
    fun sessionId(timestamp: Long): String?
}

object NoOpSplunkSessionManager : ISplunkSessionManager {
    override val sessionId: String? = null
    override val sessionSnapshot: SessionSnapshot? = null
    override val previousSessionId: String? = null
    override val sessionStart: Long? = null
    override val sessionLastActivity: Long? = null
    override val sessionListeners: MutableSet<SessionListener> = mutableSetOf()
    override fun install(application: Application) = Unit
    override fun trackSessionActivity() = Unit
    override fun reset() = Unit

    override fun sessionId(timestamp: Long): String? = null
}

class SplunkSessionManager internal constructor(
    private val agentStorage: IAgentStorage,
    private val includeBackgroundOnlySessions: Boolean = true,
    private val onSessionCreated: (String, String?, Long) -> Unit = { _, _, _ -> }
) : ISplunkSessionManager {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val appStateObserver = AppStateObserver

    private var sessionValidityWatcher: ScheduledFuture<*>? = null
    private var sessionLengthWatcher: ScheduledFuture<*>? = null
    private var isForeground = false

    private val sessionIds: MutableList<SessionIdStorageData> by lazy {
        agentStorage.readSessionIds().toMutableList()
    }

    override val sessionSnapshot: SessionSnapshot?
        @Synchronized get() {
            val id = sessionId ?: return null
            val start = sessionIds.lastOrNull { it.id == id }?.validFrom ?: System.currentTimeMillis()
            val lastActivity = agentStorage.readSessionLastActivity() ?: start
            return SessionSnapshot(id, start, lastActivity)
        }

    override val sessionId: String?
        @Synchronized get() = if (includeBackgroundOnlySessions) {
            createNewSessionIfNeeded()
        } else {
            readCurrentSessionId()
        }

    override var previousSessionId: String? = null
        private set

    override val sessionStart: Long?
        get() = sessionSnapshot?.sessionStart

    override val sessionLastActivity: Long?
        get() = sessionSnapshot?.sessionLastActivity

    override val sessionListeners: MutableSet<SessionListener> = HashSet()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var sessionBackgroundTimeout: Long = DEFAULT_SESSION_BACKGROUND_TIMEOUT

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var maxSessionLength: Long = DEFAULT_SESSION_LENGTH

    override fun install(application: Application) {
        if (includeBackgroundOnlySessions) {
            createNewSessionIfNeeded()
        }
        appStateObserver.listeners.add(AppStateObserverListener())
        appStateObserver.attach(application)

        if (!includeBackgroundOnlySessions && appStateObserver.hasStartedActivity) {
            enterForeground()
        }
    }

    @Synchronized
    override fun reset() {
        cancelSessionWatcher()
        cancelSessionLengthWatcher()
        isForeground = false
        previousSessionId = null
        clearLastSession()
    }

    override fun sessionId(timestamp: Long): String? = sessionIds
        .filter { it.validFrom <= timestamp }
        .maxByOrNull { it.validFrom }
        ?.id

    /** Reads an existing session without creating one. A late timeout task cannot make an expired ID valid again. */
    @Synchronized
    private fun readCurrentSessionId(): String? {
        val savedId = agentStorage.readSessionId() ?: return null
        val now = System.currentTimeMillis()
        val backgroundDeadline = agentStorage.readSessionValidUntilInBackground()
        val lengthDeadline = agentStorage.readSessionValidUntil()

        if (backgroundDeadline != null && backgroundDeadline <= now) {
            endCurrentSession(now)
            return null
        }
        if (lengthDeadline == null) {
            endCurrentSession(now)
            return null
        }
        if (lengthDeadline <= now) {
            // A foreground session is rotated by the length watcher, not by an ID read.
            // Keep its old ID internally until that watcher runs, but never return it.
            if (!isForeground || !appStateObserver.hasStartedActivity) {
                endCurrentSession(now)
            }
            return null
        }
        return savedId
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
        deleteSessionLastActivity()

        val newSessionId = SessionId.generate()
        previousSessionId = savedSessionId
        agentStorage.writeSessionId(newSessionId)
        agentStorage.writeSessionValidUntil(now + maxSessionLength)
        onSessionCreated(newSessionId, previousSessionId, now)
        sessionIds.add(SessionIdStorageData(newSessionId, now))
        agentStorage.writeSessionIds(sessionIds)
        sessionListeners.forEachFast { it.onSessionChanged(newSessionId, now) }
        return newSessionId
    }

    override fun trackSessionActivity() {
        if (includeBackgroundOnlySessions || sessionId != null) {
            agentStorage.writeSessionLastActivity(System.currentTimeMillis())
        }
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

    @Synchronized
    private fun endCurrentSession(timestamp: Long) {
        if (agentStorage.readSessionId() == null) return
        cancelSessionLengthWatcher()
        clearLastSession()
        previousSessionId = null
        sessionListeners.forEachFast { it.onSessionEnded(timestamp) }
    }

    @Synchronized
    private fun enterForeground() {
        if (!appStateObserver.hasStartedActivity) return

        // Check the deadline before clearing it. An Activity created after the grace period
        // must not resurrect the old session.
        val currentId = readCurrentSessionId()
        isForeground = true
        cancelSessionWatcher()
        if (currentId != null) {
            deleteSessionInBackgroundValidationTime()
            return
        }

        cancelSessionLengthWatcher()
        createForegroundSession(previousId = null)
    }

    @Synchronized
    private fun createForegroundSession(previousId: String?) {
        clearLastSession()
        val timestamp = System.currentTimeMillis()
        val newId = SessionId.generate()
        previousSessionId = previousId
        agentStorage.writeSessionId(newId)
        agentStorage.writeSessionValidUntil(timestamp + maxSessionLength)
        onSessionCreated(newId, previousId, timestamp)
        sessionIds.add(SessionIdStorageData(newId, timestamp))
        agentStorage.writeSessionIds(sessionIds)
        scheduleSessionLengthWatcher(newId, maxSessionLength)
        sessionListeners.forEachFast { it.onSessionChanged(newId, timestamp) }
    }

    private fun scheduleSessionLengthWatcher(sessionId: String, delay: Long) {
        cancelSessionLengthWatcher()
        sessionLengthWatcher = executor.safeSchedule(delay) {
            rotateSessionIfNeeded(sessionId)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Synchronized
    internal fun rotateSessionIfNeeded(expectedId: String) {
        if (agentStorage.readSessionId() != expectedId) return
        val now = System.currentTimeMillis()
        val deadline = agentStorage.readSessionValidUntil() ?: return
        if (deadline > now) {
            scheduleSessionLengthWatcher(expectedId, deadline - now)
            return
        }

        if (isForeground && appStateObserver.hasStartedActivity) {
            createForegroundSession(previousId = expectedId)
        } else {
            endCurrentSession(now)
        }
    }

    private fun cancelSessionLengthWatcher() {
        sessionLengthWatcher?.cancel(false)
        sessionLengthWatcher = null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Synchronized
    internal fun watchSessionInBackgroundValidity() {
        if (!includeBackgroundOnlySessions && readCurrentSessionId() == null) return
        saveSessionInBackgroundValidationTime()

        sessionValidityWatcher = executor.safeSchedule(sessionBackgroundTimeout) {
            expireSessionInBackground()
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Synchronized
    internal fun expireSessionInBackground() {
        sessionValidityWatcher = null
        if (includeBackgroundOnlySessions) {
            deleteSessionInBackgroundValidationTime()
            deleteSessionValidationTime()
            createNewSessionIfNeeded()
            return
        }
        if (isForeground) return

        val deadline = agentStorage.readSessionValidUntilInBackground() ?: return
        val remaining = deadline - System.currentTimeMillis()
        if (remaining > 0) {
            sessionValidityWatcher = executor.safeSchedule(remaining) { expireSessionInBackground() }
        } else {
            readCurrentSessionId()
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

        fun onSessionEnded(timestamp: Long) {}
    }

    private inner class AppStateObserverListener : AppStateObserver.Listener {

        override fun onAppStarted() {
            if (includeBackgroundOnlySessions) deleteSessionInBackgroundValidationTime()
        }

        override fun onAppBackgrounded() {
            backgroundApp()
        }

        override fun onAppForegrounded() {
            if (includeBackgroundOnlySessions) {
                cancelSessionWatcher()
            } else {
                enterForeground()
            }
        }

        override fun onAppClosed() {
            if (includeBackgroundOnlySessions) {
                cancelSessionWatcher()
                saveSessionInBackgroundValidationTime()
            }
        }
    }

    @Synchronized
    private fun backgroundApp() {
        isForeground = false
        watchSessionInBackgroundValidity()
    }

    private companion object {
        const val DEFAULT_SESSION_BACKGROUND_TIMEOUT = 15L * 60L * 1000L // 15m
        const val DEFAULT_SESSION_LENGTH = 4L * 60L * 60L * 1000L // 4h
    }
}
