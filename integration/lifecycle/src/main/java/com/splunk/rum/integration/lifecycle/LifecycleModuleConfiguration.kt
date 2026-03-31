/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.lifecycle

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.lifecycle.model.LifecycleAction

/**
 * UI lifecycle module configuration.
 *
 * This module captures Android Activity and Fragment lifecycle transitions and emits them
 * as OpenTelemetry `device.app.ui.lifecycle` events.
 *
 * @property isEnabled Whether the module is enabled. Default is true.
 * @property allowedEvents Set of lifecycle actions to track. Default is [CORE_LIFECYCLE_EVENTS].
 */
data class LifecycleModuleConfiguration @JvmOverloads constructor(
    val isEnabled: Boolean = true,
    val allowedEvents: Set<LifecycleAction> = DEFAULT_ALLOWED_EVENTS
) : ModuleConfiguration {

    override val name: String = "lifecycle"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "allowedEvents" to allowedEvents.joinToString(",", "[", "]") { it.name }
    )

    companion object {
        /**
         * Core lifecycle events without pre/post variants. Covers the main lifecycle transitions
         * for both Activities and Fragments.
         */
        val CORE_LIFECYCLE_EVENTS: Set<LifecycleAction> = setOf(
            LifecycleAction.CREATED,
            LifecycleAction.STARTED,
            LifecycleAction.RESUMED,
            LifecycleAction.PAUSED,
            LifecycleAction.STOPPED,
            LifecycleAction.DESTROYED,
            LifecycleAction.ATTACHED,
            LifecycleAction.VIEW_CREATED,
            LifecycleAction.VIEW_DESTROYED,
            LifecycleAction.DETACHED
        )

        /**
         * Every lifecycle callback including pre/post variants (API 29+).
         * Full diagnostic detail.
         */
        val ALL_LIFECYCLE_EVENTS: Set<LifecycleAction> = LifecycleAction.values().toSet()

        /**
         * Default: tracks core lifecycle events (no pre/post variants).
         */
        val DEFAULT_ALLOWED_EVENTS: Set<LifecycleAction> = CORE_LIFECYCLE_EVENTS
    }
}
