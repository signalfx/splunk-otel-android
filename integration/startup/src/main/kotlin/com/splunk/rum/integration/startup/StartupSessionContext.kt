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

internal data class StartupSession(val id: String, val previousId: String?)

/** Holds the session chosen at foreground until the matching App Start finishes. */
internal class StartupSessionContext {
    private var firstSession: StartupSession? = null
    private var foregroundSession: StartupSession? = null
    private var waitingForSession = false
    private var firstStartReported = false

    @Synchronized
    fun onForeground(session: StartupSession?) {
        waitingForSession = session == null
        if (session != null) setForegroundSession(session)
    }

    @Synchronized
    fun onSessionCreated(session: StartupSession) {
        if (waitingForSession || (!firstStartReported && firstSession == null)) {
            setForegroundSession(session)
            waitingForSession = false
        }
    }

    @Synchronized
    fun sessionForStart(): StartupSession? = if (firstStartReported) foregroundSession else firstSession

    @Synchronized
    fun onStartReported(session: StartupSession) {
        firstStartReported = true
        if (firstSession == session) firstSession = null
        if (foregroundSession == session) foregroundSession = null
    }

    @Synchronized
    fun onBackground() {
        if (!firstStartReported) firstSession = null
        foregroundSession = null
        waitingForSession = false
    }

    private fun setForegroundSession(session: StartupSession) {
        foregroundSession = session
        if (!firstStartReported && firstSession == null) firstSession = session
    }
}
