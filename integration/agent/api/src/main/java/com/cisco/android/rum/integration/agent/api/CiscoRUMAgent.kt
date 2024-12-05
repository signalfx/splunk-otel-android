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

package com.cisco.android.rum.integration.agent.api

import android.app.Application
import com.cisco.android.rum.integration.agent.api.internal.MRUMAgentCore
import com.cisco.android.rum.integration.agent.module.ModuleConfiguration

/**
 * The [CiscoRUMAgent] class is responsible for initializing and providing access to the RUM agent.
 * Agent is initialized through the [install] method.
 */
class CiscoRUMAgent private constructor() {

    companion object {

        private var instanceInternal: CiscoRUMAgent? = null

        /**
         * Provides access to the initialized instance of [CiscoRUMAgent].
         *
         * @return The single instance of [CiscoRUMAgent] that has been initialized.
         * @throws RuntimeException if the [install] method has not been called prior to accessing the instance.
         */
        @get:JvmStatic
        val instance: CiscoRUMAgent
            get() = instanceInternal ?: throw RuntimeException("Must call install() before fetching instance")


        /**
         * Initializes the RUM agent with the provided application context and configurations.
         * This method must be called before accessing the [instance] property.
         *
         * If the RUM agent has already been initialized, this method returns the existing instance.
         *
         * @param application The application context used to initialize the RUM agent.
         * @param agentConfiguration Configuration parameters for the RUM agent.
         * @param moduleConfigurations An array of module configurations.
         * @return The initialized [CiscoRUMAgent] instance.
         */
        @JvmStatic
        fun install(application: Application, agentConfiguration: AgentConfiguration, vararg moduleConfigurations: ModuleConfiguration): CiscoRUMAgent {
            if (instanceInternal != null)
                return instance

            MRUMAgentCore.install(application, agentConfiguration, moduleConfigurations.toList())

            instanceInternal = CiscoRUMAgent()

            return instance
        }
    }
}
