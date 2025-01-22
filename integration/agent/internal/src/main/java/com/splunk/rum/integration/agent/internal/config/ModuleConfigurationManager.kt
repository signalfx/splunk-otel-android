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

package com.splunk.rum.integration.agent.internal.config

import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.sdk.common.storage.IAgentStorage

class ModuleConfigurationManager internal constructor(
    private val agentStorage: IAgentStorage
) {

    private var serverClock: ServerClock? = null

    var configurations: List<ModuleConfiguration> = emptyList()
        private set

    val listeners: MutableSet<Listener> = HashSet()

    fun currentTimeMillis(): Long {
        return serverClock?.currentTimeMillis() ?: System.currentTimeMillis()
    }

    internal fun setup(moduleConfigurations: List<ModuleConfiguration>) {
        this.configurations = moduleConfigurations
    }

    interface Listener
}
