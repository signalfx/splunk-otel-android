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

package com.splunk.rum.integration.networkmonitor

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.android.instrumentation.InstallationContext
import com.splunk.rum.integration.agent.internal.extension.find
import io.opentelemetry.android.instrumentation.network.NetworkChangeInstrumentation

internal object NetworkMonitorIntegration {

    private const val TAG = "NetworkMonitorIntegration"
    private const val MODULE_NAME = "networkMonitor"

    private val defaultModuleConfiguration = NetworkMonitorModuleConfiguration()
    private var moduleConfiguration = defaultModuleConfiguration

    init {
        Logger.d(TAG, "init()")
        AgentIntegration.registerModuleInitializationStart(MODULE_NAME)
    }

    fun attach(context: Context) {
        Logger.d(TAG, "attach()")
        AgentIntegration.obtainInstance(context).listeners += installationListener
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(
            context: Context,
            oTelInstallationContext: InstallationContext,
            moduleConfigurations: List<ModuleConfiguration>) {
            Logger.d(TAG, "onInstall()")

            moduleConfiguration = moduleConfigurations.find< NetworkMonitorModuleConfiguration>() ?: defaultModuleConfiguration

            //install Network Monitor instrumentation if isEnabled is true
            if(moduleConfiguration.isEnabled){
                NetworkChangeInstrumentation().install(oTelInstallationContext)
            }

            AgentIntegration.registerModuleInitializationEnd(MODULE_NAME)
        }
    }
}
