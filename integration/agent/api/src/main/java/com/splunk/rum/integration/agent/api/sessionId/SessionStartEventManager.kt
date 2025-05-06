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

package com.splunk.rum.integration.agent.api.sessionId

import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager

internal class SessionStartEventManager(sessionManager: SplunkSessionManager) {

    init {
        Logger.d(TAG, "init()")

        sessionManager.sessionListeners += object : SplunkSessionManager.SessionListener {
            override fun onSessionChanged(sessionId: String) {
            }
        }
    }

    companion object {
        private const val TAG = "SessionStartEventManager"
        private var instanceInternal: SessionStartEventManager? = null
        fun obtainInstance(sessionManager: SplunkSessionManager): SessionStartEventManager {
            if (instanceInternal == null)
                instanceInternal =
                    SessionStartEventManager(sessionManager)

            return requireNotNull(instanceInternal)
        }
    }
}
