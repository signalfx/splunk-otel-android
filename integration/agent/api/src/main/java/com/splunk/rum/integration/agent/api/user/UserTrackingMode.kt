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

import com.splunk.rum.integration.agent.internal.user.InternalUserTrackingMode

enum class UserTrackingMode {
    NO_TRACKING,
    ANONYMOUS_TRACKING
}

internal fun UserTrackingMode.toInternal(): InternalUserTrackingMode = when (this) {
    UserTrackingMode.NO_TRACKING -> InternalUserTrackingMode.NO_TRACKING
    UserTrackingMode.ANONYMOUS_TRACKING -> InternalUserTrackingMode.ANONYMOUS_TRACKING
}

internal fun InternalUserTrackingMode.toPublic(): UserTrackingMode = when (this) {
    InternalUserTrackingMode.NO_TRACKING -> UserTrackingMode.NO_TRACKING
    InternalUserTrackingMode.ANONYMOUS_TRACKING -> UserTrackingMode.ANONYMOUS_TRACKING
}