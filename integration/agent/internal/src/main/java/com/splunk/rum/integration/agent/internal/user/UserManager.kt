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

package com.splunk.rum.integration.agent.internal.user

import com.splunk.rum.common.storage.IAgentStorage
import com.splunk.rum.integration.agent.internal.id.UserId

interface IUserManager {
    var trackingMode: InternalUserTrackingMode
    val userId: String?
}

class UserManager(userTrackingMode: InternalUserTrackingMode, private val agentStorage: IAgentStorage) : IUserManager {

    init {
        updateAnonymousUserIdForTrackingMode(userTrackingMode)
    }

    override val userId: String?
        get() = when (trackingMode) {
            InternalUserTrackingMode.NO_TRACKING -> null
            InternalUserTrackingMode.ANONYMOUS_TRACKING -> agentStorage.readAnonymousUserId()
        }

    override var trackingMode: InternalUserTrackingMode = userTrackingMode
        set(value) {
            updateAnonymousUserIdForTrackingMode(value)
            field = value
        }

    private fun updateAnonymousUserIdForTrackingMode(trackingMode: InternalUserTrackingMode) {
        when (trackingMode) {
            InternalUserTrackingMode.NO_TRACKING -> {
                agentStorage.deleteAnonymousUserId()
            }
            InternalUserTrackingMode.ANONYMOUS_TRACKING -> {
                if (agentStorage.readAnonymousUserId() == null) {
                    agentStorage.writeAnonymousUserId(UserId.generate())
                }
            }
        }
    }
}

object NoOpUserManager : IUserManager {
    override var trackingMode: InternalUserTrackingMode = InternalUserTrackingMode.NO_TRACKING
    override val userId: String? = null
}

enum class InternalUserTrackingMode {
    NO_TRACKING,
    ANONYMOUS_TRACKING
}
