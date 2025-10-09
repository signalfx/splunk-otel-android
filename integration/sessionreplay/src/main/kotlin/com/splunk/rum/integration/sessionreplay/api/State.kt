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

import com.splunk.android.instrumentation.recording.core.api.SessionReplay
import com.splunk.rum.integration.sessionreplay.api.mapping.toSplunk

class State internal constructor(private var statusOverrider: StatusOverrideProvider) {

    /**
     * The current SDK status.
     */
    val status: Status
        get() = statusOverrider.onGetStatus() ?: SessionReplay.instance.state.status.toSplunk()

    /**
     * Screen data rendering mode.
     */
    val renderingMode: RenderingMode
        get() = SessionReplay.instance.state.renderingMode.toSplunk()

    internal interface StatusOverrideProvider {
        fun onGetStatus(): Status?
    }
}
