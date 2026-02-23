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
import com.splunk.rum.integration.agent.internal.user.IUserManager

class SessionState internal constructor(
    private val sessionConfiguration: SessionConfiguration,
    private val sessionManager: ISplunkSessionManager,
    private val userManager: IUserManager
) {
    val id: String
        get() = sessionManager.sessionId

    val samplingRate: Double
        get() = sessionConfiguration.samplingRate

    val listeners: List<SessionListener>
        get() = sessionConfiguration.listeners

    val metadata: SessionMetadata
        get() {
            val sessionSnapshot = sessionManager.sessionSnapshot
            return SessionMetadata(
                sessionId = sessionSnapshot.sessionId,
                anonymousUserId = userManager.userId,
                sessionStart = sessionSnapshot.sessionStart,
                sessionLastActivity = sessionSnapshot.sessionLastActivity
            )
        }
}
