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

package com.splunk.rum.integration.sessionreplay.api

import com.splunk.rum.integration.sessionreplay.SessionReplayModuleConfiguration

sealed interface Status {

    /**
     * Whether this is [Recording] status.
     */
    val isRecording: Boolean
        get() = this is Recording

    /**
     * SDK is recording of an user's activity.
     */
    object Recording : Status

    /**
     * SDK is not recording of an user's activity.
     *
     * @param cause Reason of not recording.
     */
    data class NotRecording internal constructor(val cause: Cause) : Status {

        enum class Cause {

            /**
             * Recording have not been started.
             *
             * @see SessionReplay.start
             */
            NOT_STARTED,

            /**
             * Recording was stopped.
             */
            STOPPED,

            /**
             * The device's Android sdk is below supported minimum.
             */
            BELOW_MIN_SDK_VERSION,

            /**
             * The device's storage is too low to start recording.
             */
            STORAGE_LIMIT_REACHED,

            /**
             *  It was impossible to start the recording because the internal
             *  database could not be open, or another internal error occurred.
             */
            INTERNAL_ERROR,

            /**
             * The recording cannot be started due to sampling.
             *
             * @see SessionReplayModuleConfiguration.samplingRate
             */
            DISABLED_BY_SAMPLING
        }
    }
}
