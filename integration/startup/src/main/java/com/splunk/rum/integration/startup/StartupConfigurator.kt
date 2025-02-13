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

package com.splunk.rum.integration.startup

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.sdk.common.otel.extensions.toInstant
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.rum.startup.ApplicationStartupTimekeeper
import com.splunk.sdk.common.otel.OpenTelemetry

internal object StartupConfigurator {

    private const val TAG = "StartupConfigurator"
    private const val MODULE_NAME = "startup"

    init {
        AgentIntegration.registerModule(MODULE_NAME)
    }

    fun attach(context: Context) {
        AgentIntegration.obtainInstance(context).listeners += installationListener

        ApplicationStartupTimekeeper.listeners += applicationStartupTimekeeperListener
    }

    private val applicationStartupTimekeeperListener = object : ApplicationStartupTimekeeper.Listener {
        override fun onColdStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(TAG, "onColdStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)")
            reportEvent(startTimestamp, endTimestamp, "cold")
        }

        override fun onWarmStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(TAG, "onWarmStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)")
            reportEvent(startTimestamp, endTimestamp, "warm")
        }

        override fun onHotStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(TAG, "onHotStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)")
            reportEvent(startTimestamp, endTimestamp, "hot")
        }

        private fun reportEvent(startTimestamp: Long, endTimestamp: Long, name: String) {
            val provider = OpenTelemetry.instance?.sdkTracerProvider ?: return

            provider.get("SplunkRum")
                .spanBuilder("AppStart")
                .setStartTimestamp(startTimestamp.toInstant())
                .setAttribute("component", "appstart")
                .setAttribute("start.type", name)
                .startSpan()
                .end(endTimestamp.toInstant())
        }
    }

    private val configManagerListener = object : ModuleConfigurationManager.Listener {
        override fun onSetup(configurations: List<ModuleConfiguration>) {
        }
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context) {
            Logger.d(TAG, "onInstall()")

            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += configManagerListener
        }
    }
}
