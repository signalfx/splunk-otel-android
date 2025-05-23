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

    class State internal constructor(private val userManager: IUserManager) {
        val trackingMode: TrackingMode
            get() = userManager.trackingMode.toPublic()
    }

    class Preferences internal constructor(private val userManager: IUserManager) {
        var trackingMode: TrackingMode? = null
            set(value) {
                field = value
                value?.let {
                    userManager.trackingMode = it.toInternal()
                }
            }
    }

    data class Configuration(val trackingMode: TrackingMode = TrackingMode.NoTracking)

    enum class TrackingMode {
        NoTracking,
        AnonymousTracking
    }
}

private fun User.TrackingMode.toInternal(): InternalUserTrackingMode = when (this) {
    User.TrackingMode.NoTracking -> InternalUserTrackingMode.NoTracking
    User.TrackingMode.AnonymousTracking -> InternalUserTrackingMode.AnonymousTracking
}

private fun InternalUserTrackingMode.toPublic(): User.TrackingMode = when (this) {
    InternalUserTrackingMode.NoTracking -> User.TrackingMode.NoTracking
    InternalUserTrackingMode.AnonymousTracking -> User.TrackingMode.AnonymousTracking
}
