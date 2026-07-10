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
 * as OpenTelemetry `app.ui.lifecycle` events.
 *
 * @property isEnabled Whether the module is enabled. Default is true.
 * @property allowedEvents Set of lifecycle actions to track. Default is [MAIN_LIFECYCLE_EVENTS].
 */
data class LifecycleModuleConfiguration @JvmOverloads constructor(
    val isEnabled: Boolean = true,
    val allowedEvents: Set<LifecycleAction> = MAIN_LIFECYCLE_EVENTS
) : ModuleConfiguration {

    override val name: String = "lifecycle"

    override val attributes: List<Pair<String, String>> = listOf(
        "enabled" to isEnabled.toString(),
        "allowedEvents" to allowedEvents.joinToString(",", "[", "]") { it.name }
    )

    companion object {
        /**
         * The 10 main lifecycle transitions for Activities and Fragments,
         * without pre/post variants. This is the default.
         */
        val MAIN_LIFECYCLE_EVENTS: Set<LifecycleAction> = setOf(
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
         * Pre/post lifecycle variants only (API 29+).
         * Use alongside [MAIN_LIFECYCLE_EVENTS] for full detail, or on its own.
         */
        val PRE_POST_LIFECYCLE_EVENTS: Set<LifecycleAction> = setOf(
            LifecycleAction.PRE_CREATED,
            LifecycleAction.POST_CREATED,
            LifecycleAction.PRE_STARTED,
            LifecycleAction.POST_STARTED,
            LifecycleAction.PRE_RESUMED,
            LifecycleAction.POST_RESUMED,
            LifecycleAction.PRE_PAUSED,
            LifecycleAction.POST_PAUSED,
            LifecycleAction.PRE_STOPPED,
            LifecycleAction.POST_STOPPED,
            LifecycleAction.PRE_DESTROYED,
            LifecycleAction.POST_DESTROYED,
            LifecycleAction.PRE_ATTACHED
        )

        /**
         * Every lifecycle callback including pre/post variants (API 29+).
         * Equivalent to [MAIN_LIFECYCLE_EVENTS] + [PRE_POST_LIFECYCLE_EVENTS].
         */
        val ALL_LIFECYCLE_EVENTS: Set<LifecycleAction> = LifecycleAction.values().toSet()
    }
}
