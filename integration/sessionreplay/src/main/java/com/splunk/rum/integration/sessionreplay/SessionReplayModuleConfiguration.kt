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

package com.splunk.rum.integration.sessionreplay

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration

/**
 * Session replay module configuration.
 *
 * @property isEnabled Whether session replay is enabled and recording can be started.
 * @property samplingRate The sampling rate for session replay.
 * 0 means session replay recording cannot be effectively enabled.
 * 0.2 means that, if session replay itself is enabled, only one fifth of sessions can be recorded.
 * 1 means that, if session replay is enabled, all sessions can be recorded. Default value is 1.0.
 */
class SessionReplayModuleConfiguration(val isEnabled: Boolean = true, val samplingRate: Float = 1.0f) :
    ModuleConfiguration {

    override val name: String = "sessionReplay"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "samplingRate" to samplingRate.toString()
    )
}
