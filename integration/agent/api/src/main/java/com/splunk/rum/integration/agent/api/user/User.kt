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

package com.splunk.rum.integration.agent.api.user

import com.splunk.rum.integration.agent.internal.user.IUserManager
import com.splunk.rum.integration.agent.internal.user.InternalUserTrackingMode

/**
 * Class representing a user.
 */
class User internal constructor(userManager: IUserManager) {

    val state: State = State(userManager)

    val preferences: Preferences = Preferences(userManager)

    class State internal constructor(
        private val userManager: IUserManager
    ) {

        val trackingMode: TrackingMode
            get() = userManager.trackingMode.toPublic()
    }

    class Preferences internal constructor(
        private val userManager: IUserManager
    ) {

        var trackingMode: TrackingMode? = null
            set(value) {
                field = value
                value?.let {
                    userManager.trackingMode = it.toInternal()
                }
            }
    }

    data class Configuration @JvmOverloads constructor(
        val trackingMode: TrackingMode = TrackingMode.NO_TRACKING
    )

    enum class TrackingMode {
        NO_TRACKING,
        ANONYMOUS_TRACKING
    }
}

private fun User.TrackingMode.toInternal(): InternalUserTrackingMode = when (this) {
    User.TrackingMode.NO_TRACKING -> InternalUserTrackingMode.NO_TRACKING
    User.TrackingMode.ANONYMOUS_TRACKING -> InternalUserTrackingMode.ANONYMOUS_TRACKING
}

private fun InternalUserTrackingMode.toPublic(): User.TrackingMode = when (this) {
    InternalUserTrackingMode.NO_TRACKING -> User.TrackingMode.NO_TRACKING
    InternalUserTrackingMode.ANONYMOUS_TRACKING -> User.TrackingMode.ANONYMOUS_TRACKING
}
