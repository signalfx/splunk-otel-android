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

package com.splunk.rum.integration.sessionreplay.api.mapping

import com.cisco.android.instrumentation.recording.core.api.Status as CommonStatus
import com.splunk.rum.integration.sessionreplay.api.Status

internal fun CommonStatus.toSplunk(): Status = when (val status = this) {
    is CommonStatus.NotRecording -> {
        when (status.cause) {
            CommonStatus.NotRecording.Cause.NOT_STARTED -> Status.NotRecording(
                cause = Status.NotRecording.Cause.NOT_STARTED
            )
            CommonStatus.NotRecording.Cause.STOPPED -> Status.NotRecording(
                cause = Status.NotRecording.Cause.STOPPED
            )
            CommonStatus.NotRecording.Cause.BELOW_MIN_SDK_VERSION -> Status.NotRecording(
                cause = Status.NotRecording.Cause.BELOW_MIN_SDK_VERSION
            )
            CommonStatus.NotRecording.Cause.STORAGE_LIMIT_REACHED -> Status.NotRecording(
                cause = Status.NotRecording.Cause.STORAGE_LIMIT_REACHED
            )
            CommonStatus.NotRecording.Cause.INTERNAL_ERROR -> Status.NotRecording(
                cause = Status.NotRecording.Cause.INTERNAL_ERROR
            )
        }
    }

    CommonStatus.Recording -> Status.Recording
}
