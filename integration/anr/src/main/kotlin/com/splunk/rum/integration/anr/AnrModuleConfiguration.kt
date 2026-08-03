/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.rum.integration.anr

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import java.time.Duration

/**
 * ANR module configuration.
 *
 * @property isEnabled Whether the module is enabled.
 * @property pollingInterval How long the main thread must be unresponsive before an ANR is reported.
 *                           Default is 5 seconds.
 */
@Suppress("NewApi") // Duration requires API 26 or core library desugaring
data class AnrModuleConfiguration @JvmOverloads constructor(
    val isEnabled: Boolean = true,
    val pollingInterval: Duration = DEFAULT_POLLING_INTERVAL
) : ModuleConfiguration {

    override val name: String = "anr"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "pollingInterval" to pollingInterval.toString()
    )

    companion object {
        /** Default ANR detection threshold. */
        val DEFAULT_POLLING_INTERVAL: Duration = Duration.ofSeconds(5)
    }
}
