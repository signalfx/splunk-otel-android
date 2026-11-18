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

package com.splunk.rum.integration.startup

/** Holds the session chosen at foreground until the matching App Start finishes. */
internal class StartupSessionContext {
    private var firstSessionId: String? = null
    private var foregroundSessionId: String? = null
    private var waitingForSession = false
    private var firstStartReported = false

    @Synchronized
    fun onForeground(sessionId: String?) {
        waitingForSession = sessionId == null
        if (sessionId != null) setForegroundSession(sessionId)
    }

    @Synchronized
    fun onSessionCreated(sessionId: String) {
        if (waitingForSession || (!firstStartReported && firstSessionId == null)) {
            setForegroundSession(sessionId)
            waitingForSession = false
        }
    }

    @Synchronized
    fun sessionIdForStart(): String? = if (firstStartReported) foregroundSessionId else firstSessionId

    @Synchronized
    fun onStartReported() {
        firstStartReported = true
        firstSessionId = null
        foregroundSessionId = null
    }

    @Synchronized
    fun onBackground() {
        if (!firstStartReported) firstSessionId = null
        foregroundSessionId = null
        waitingForSession = false
    }

    private fun setForegroundSession(sessionId: String) {
        foregroundSessionId = sessionId
        if (!firstStartReported && firstSessionId == null) firstSessionId = sessionId
    }
}
