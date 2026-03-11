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

import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.utils.extensions.toBase64
import org.json.JSONObject

class Session internal constructor(val userManager: IUserManager, override val state: SessionState) : ISession {

    override val metadata: String
        get(): String {
            val json = JSONObject().apply {
                put("sessionId", state.id)
                put("anonymousUserId", userManager.userId)
                put("sessionStart", state.sessionStart)
                put("sessionLastActivity", state.sessionLastActivity)
            }

            return json.toString().toBase64()
        }
}
