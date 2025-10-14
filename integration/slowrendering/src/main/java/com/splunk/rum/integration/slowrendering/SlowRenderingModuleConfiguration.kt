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

package com.splunk.rum.integration.slowrendering

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import java.time.Duration

/**
 * Configuration for the slow rendering detection module.
 *
 * This module enables detection of slow or frozen application user interface
 * The instrumentation operates by periodically polling for frame metrics, by default every second.
 *
 * @property isEnabled Whether the slow rendering detection is enabled.
 * @property interval The time period between polling for slow or frozen frames.
 *                    A shorter interval provides more frequent checks but uses more resources.
 *                    Default is 1 second.
 */
@Suppress("NewApi") // Duration.ofSeconds() requires API 26 or core library desugaring
data class SlowRenderingModuleConfiguration @JvmOverloads constructor(
    val isEnabled: Boolean = true,
    val interval: Duration = Duration.ofSeconds(1)
) : ModuleConfiguration {

    override val name: String = "slowrendering"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "interval" to interval.toString()
    )
}
