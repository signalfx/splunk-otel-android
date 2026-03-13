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
 * Configuration for the navigation module.
 *
 * - [ON_SCREEN_ACTIVE]: Emit only when a screen becomes active (onResumed). One event per
 *   "arrival" on a screen. Use for legacy-like volume and semantics.
 * - [ALL_CHANGES]: Emit on every visible-screen state change (resumed and paused).
 *   Produces more events per user navigation because both Activity and Fragment
 *   callbacks update state; use when you want maximum granularity.
 */
enum class NavigationEmissionPolicy {
    ON_SCREEN_ACTIVE,
    ALL_CHANGES;

    internal val emitOnResumed: Boolean get() = true
    internal val emitOnPaused: Boolean get() = this == ALL_CHANGES
}

/**
 * Navigation module configuration.
 *
 * This module tracks screen navigation and emits OpenTelemetry `device.app.ui.navigation` events
 * for screen arrivals (and optionally all visible-screen transitions when automated tracking is enabled).
 *
 * @property isEnabled Whether the module is enabled. Default is true.
 * @property isAutomatedTrackingEnabled Whether Fragment and Activity lifecycle tracking is enabled. Default is false.
 * @property emissionPolicy When to emit navigation events. [NavigationEmissionPolicy.ON_SCREEN_ACTIVE]
 *   (default) emits only on screen resumed (one per arrival); [NavigationEmissionPolicy.ALL_CHANGES]
 *   emits on every visible-screen change.
 */
data class NavigationModuleConfiguration @JvmOverloads constructor(
    val isEnabled: Boolean = true,
    val isAutomatedTrackingEnabled: Boolean = false,
    val emissionPolicy: NavigationEmissionPolicy = NavigationEmissionPolicy.ON_SCREEN_ACTIVE
) : ModuleConfiguration {

    override val name: String = "navigation"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "isAutomatedTrackingEnabled" to isAutomatedTrackingEnabled.toString(),
        "emissionPolicy" to emissionPolicy.name
    )
}
