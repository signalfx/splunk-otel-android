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
import com.splunk.rum.integration.agent.api.SplunkRUMAgent.Companion.install
import com.splunk.rum.integration.agent.api.internal.MRUMAgentCore
import com.splunk.rum.integration.agent.api.internal.SplunkRumAgentCore
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.sdk.common.otel.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import java.net.URL
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

        @JvmStatic
        @Deprecated("Use install()")
        fun builder(): Builder {
            return Builder()
        }
    }

    @Deprecated("Use install()")
    class Builder internal constructor() {

        private var accessToken: String? = null
        private var applicationName: String? = null
        private var deploymentEnvironment: String? = null
        private var realm: String? = null
        private var beaconEndpoint: String? = null
        private var enableDebug: Boolean = false
        private var attributes: Attributes? = null
        private var enableSessionBasedSampling: Boolean = false

        fun setRumAccessToken(token: String): Builder {
            accessToken = token
            return this
        }

        fun setApplicationName(name: String): Builder {
            applicationName = name
            return this
        }

        fun setDeploymentEnvironment(environment: String): Builder {
            deploymentEnvironment = environment
            return this
        }

        fun setRealm(realm: String): Builder {
            if (beaconEndpoint != null)
                throw IllegalStateException("setRealm can not be set when setBeaconEndpoint was called")

            this.realm = realm
            return this
        }

        fun setBeaconEndpoint(endpoint: String): Builder {
            if (beaconEndpoint != null)
                throw IllegalStateException("setBeaconEndpoint can not be set when setRealm was called")

            beaconEndpoint = endpoint
            return this
        }

        fun enableDebug(debug: Boolean): Builder {
            enableDebug = debug
            return this
        }

        fun setGlobalAttributes(attributes: Attributes): Builder {
            this.attributes = attributes
            return this
        }

        fun filterSpans(): Builder { // TODO
            return this
        }

        fun limitDiskUsageMegabytes(): Builder { // TODO
            return this
        }

        fun enableSessionBasedSampling(enable: Boolean): Builder { // TODO
            enableSessionBasedSampling = enable
            return this
        }

        fun disableSubprocessInstrumentation(): Builder { // TODO
            return this
        }

        fun enableBackgroundInstrumentationDeferredUntilForeground(): Builder { //TODO
            return this
        }

        @Deprecated("This is no longer supported")
        fun enableDiskBuffering(enable: Boolean): Builder {
            return this
        }

        fun build(application: Application): SplunkRUMAgent {
            val agent = install(
                application,
                agentConfiguration = AgentConfiguration(
                    url = URL(""), // TODO
                    appName = applicationName,
                    isDebugLogsEnabled = enableDebug
                )
            )

            // TODO setRumAccessToken
            // TODO setDeploymentEnvironment
            // TODO setRealm
            // TODO setBeaconEndpoint
            // TODO setGlobalAttributes
            // TODO filterSpans
            // TODO limitDiskUsageMegabytes
            // TODO enableSessionBasedSampling
            // TODO disableSubprocessInstrumentation
            // TODO enableBackgroundInstrumentationDeferredUntilForeground

            return agent
        }
    }
}
