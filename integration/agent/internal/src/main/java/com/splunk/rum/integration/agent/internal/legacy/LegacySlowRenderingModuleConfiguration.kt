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

package com.splunk.rum.integration.agent.internal.legacy

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import java.time.Duration

@Deprecated("Only to support legacy API, can be removed with legacy API.")
class LegacySlowRenderingModuleConfiguration(
    val isEnabled: Boolean = true,
    @Suppress("NewApi") //Requires API 26 or core library desugaring
    val interval: Duration = Duration.ofSeconds(1)
) : ModuleConfiguration {

    override val name: String = "slowrendering"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "interval" to interval.toString()
    )
}
