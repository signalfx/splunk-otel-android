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

package com.cisco.android.rum.integration.networkrequest.configurer

import android.content.Context
import com.cisco.android.rum.integration.agent.internal.AgentIntegration
import com.cisco.android.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.cisco.android.rum.integration.agent.internal.config.RemoteModuleConfiguration
import com.cisco.android.rum.integration.agent.internal.extension.find
import com.smartlook.sdk.common.logger.Logger
import com.smartlook.sdk.common.utils.extensions.optBooleanNull
import com.smartlook.sdk.log.LogAspect

object NetworkRequestConfigurer {

    private const val TAG = "NetworkRequestConfigurer"
    private const val MODULE_NAME = "networkTracing"
    private const val DEFAULT_IS_ENABLED = true

    @JvmField
    var isNetworkTracingEnabled: Boolean = DEFAULT_IS_ENABLED

    init {
        Logger.privateD(LogAspect.SDK_METHODS, TAG, { "init()" })
        AgentIntegration.registerModule(MODULE_NAME)
    }

    fun attach(context: Context) {
        Logger.privateD(LogAspect.SDK_METHODS, TAG, { "attach()" })
        AgentIntegration.obtainInstance(context).listeners += installationListener
    }

    private val configManagerListener = object : ModuleConfigurationManager.Listener {
        override fun onRemoteModuleConfigurationsChanged(manager: ModuleConfigurationManager, remoteConfigurations: List<RemoteModuleConfiguration>) {
            Logger.privateD(LogAspect.SDK_METHODS, TAG, { "onRemoteModuleConfigurationsChanged(remoteConfigurations: $remoteConfigurations)" })
            setModuleConfiguration(remoteConfigurations)
        }
    }

    private fun setModuleConfiguration(remoteConfigurations: List<RemoteModuleConfiguration>) {
        Logger.privateD(LogAspect.SDK_METHODS, TAG, { "setModuleConfiguration(remoteConfigurations: $remoteConfigurations)" })

        val remoteConfig = remoteConfigurations.find(MODULE_NAME)?.config

        isNetworkTracingEnabled = remoteConfig?.optBooleanNull("enabled") ?: DEFAULT_IS_ENABLED
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context) {
            Logger.privateD(LogAspect.SDK_METHODS, TAG, { "onInstall()" })

            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += configManagerListener

            setModuleConfiguration(integration.moduleConfigurationManager.remoteConfigurations)
        }
    }
}
