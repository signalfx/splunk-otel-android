/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.agent.api.session

import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager

/**
 * Exposes immutable accessors for the current RUM session state.
 */
class SessionState internal constructor(
    private val sessionConfiguration: SessionConfiguration,
    private val sessionManager: ISplunkSessionManager
) {
    /**
     * Unique identifier of the current session.
     */
    val id: String
        get() = sessionManager.sessionId

    /**
     * Session start time as a Unix timestamp in milliseconds.
     */
    val start: Long
        get() = sessionManager.sessionStart

    /**
     * Time of the last recorded session activity as a Unix timestamp in milliseconds.
     */
    val lastActivity: Long
        get() = sessionManager.sessionLastActivity

    /**
     * Sampling rate applied to the current session.
     */
    val samplingRate: Double
        get() = sessionConfiguration.samplingRate

    /**
     * Listeners notified when the active session changes.
     */
    val listeners: List<SessionListener>
        get() = sessionConfiguration.listeners
}
