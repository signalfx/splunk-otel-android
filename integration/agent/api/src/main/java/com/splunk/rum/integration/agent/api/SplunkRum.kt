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

package com.splunk.rum.integration.agent.api

import android.app.Application
import com.splunk.rum.integration.agent.api.internal.SplunkRumAgentCore
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.api.OpenTelemetry

/**
 * The [SplunkRum] class is responsible for initializing and providing access to the RUM agent.
 * Agent is initialized through the [install] method.
 */
class SplunkRum private constructor(
    agentConfiguration: AgentConfiguration,
    val openTelemetry: OpenTelemetry,
    val state: IState = State(agentConfiguration)
) {
    // TODO separate task
    var globalAttributes: Any = agentConfiguration.globalAttributes

    companion object {
        private val noop = SplunkRum(openTelemetry = OpenTelemetry.noop(), agentConfiguration = AgentConfiguration.noop, state = Noop)
        private var instanceInternal: SplunkRum? = null

        /**
         * Provides access to the initialized instance of [SplunkRum].
         *
         * @return The single instance of [SplunkRum] that has been initialized.
         * @throws RuntimeException if the [install] method has not been called prior to accessing the instance.
         */
        @get:JvmStatic
        val instance: SplunkRum
            get() = instanceInternal ?: noop

        /**
         * Initializes the RUM agent with the provided application context and configurations.
         * This method must be called before accessing the [instance] property.
         *
         * If the RUM agent has already been initialized, this method returns the existing instance.
         *
         * @param application The application context used to initialize the RUM agent.
         * @param agentConfiguration Configuration parameters for the RUM agent.
         * @param moduleConfigurations An array of module configurations.
         * @return The initialized [SplunkRum] instance.
         */
        @JvmStatic
        fun install(application: Application, agentConfiguration: AgentConfiguration, vararg moduleConfigurations: ModuleConfiguration): SplunkRum {
            if (instanceInternal != null)
                return instance

            val openTelemetry = SplunkRumAgentCore.install(application, agentConfiguration, moduleConfigurations.toList())

            instanceInternal = SplunkRum(agentConfiguration = agentConfiguration, openTelemetry = openTelemetry)

            return instance
        }
    }
}
