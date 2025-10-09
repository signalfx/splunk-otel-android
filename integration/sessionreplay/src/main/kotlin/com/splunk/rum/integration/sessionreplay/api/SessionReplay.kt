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

import android.os.Build
import com.splunk.android.common.logger.Logger
import com.splunk.android.instrumentation.recording.core.api.SessionReplay as CommonSessionReplay
import com.splunk.rum.integration.sessionreplay.api.mapping.toCommon
import com.splunk.rum.integration.sessionreplay.api.mapping.toSplunk

class SessionReplay internal constructor() {

    private var statusOverride: Status? = null

    /**
     * Preferred configuration. The entered values represent only the preferred configuration. The resulting state may be different according to your
     * account settings.
     *
     * @see state
     */
    val preferences: Preferences = Preferences()

    /**
     * The current SDK state. Each value is combination of default one and [preferences].
     */
    val state: State = State(StatusOverrideProvider())

    /**
     * Sensitivity configuration defines which part of screen will not be visible. Used only when [State.renderingMode] is [RenderingMode.NATIVE].
     */
    val sensitivity: Sensitivity = Sensitivity()

    /**
     * Recording mask configuration defines which part of screen will not be visible. Used only when [State.renderingMode] is [RenderingMode.NATIVE].
     */
    var recordingMask: RecordingMask?
        get() = CommonSessionReplay.instance.recordingMask?.toSplunk()
        set(value) {
            CommonSessionReplay.instance.recordingMask = value?.toCommon()
        }

    /**
     * Starts recording of a user activity.
     */
    fun start() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Logger.w(TAG, "start() - Unsupported Android version")

            statusOverride = Status.NotRecording(
                cause = Status.NotRecording.Cause.BELOW_MIN_SDK_VERSION
            )

            return
        }

        statusOverride = null
        CommonSessionReplay.instance.start()
    }

    /**
     * Stops recording of a user activity.
     */
    fun stop() {
        CommonSessionReplay.instance.stop()
    }

    private inner class StatusOverrideProvider : State.StatusOverrideProvider {
        override fun onGetStatus(): Status? = statusOverride
    }

    companion object {

        private const val TAG = "SessionReplay"

        /**
         * Returns instance of the SessionReplay.
         */
        @JvmStatic
        val instance by lazy { SessionReplay() }
    }
}
