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

package com.splunk.rum.integration.navigation

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration

/**
 * Navigation module configuration.
 *
 * This module tracks screen navigation and emits OpenTelemetry `device.app.ui.navigation` events
 * for screen arrivals when automated tracking is enabled. Detection uses onFragmentResumed /
 * onActivityResumed as the trigger (per ticket: fragment detection relies on onFragmentResumed).
 *
 * @property isEnabled Whether the module is enabled. Default is true.
 * @property isAutomatedTrackingEnabled Whether Fragment and Activity lifecycle tracking is enabled. Default is false.
 * @property navigationEventProcessor Optional processor for transforming or filtering navigation events
 *   from Compose routes before they are emitted. See [NavigationEventProcessor].
 */
data class NavigationModuleConfiguration @JvmOverloads constructor(
    val isEnabled: Boolean = true,
    val isAutomatedTrackingEnabled: Boolean = false,
    val navigationEventProcessor: NavigationEventProcessor? = null
) : ModuleConfiguration {

    override val name: String = "navigation"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "isAutomatedTrackingEnabled" to isAutomatedTrackingEnabled.toString()
    )
}
