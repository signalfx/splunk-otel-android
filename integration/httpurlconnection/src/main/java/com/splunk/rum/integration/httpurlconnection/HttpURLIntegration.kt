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

package com.splunk.rum.integration.httpurlconnection

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlInstrumentation
import com.splunk.rum.integration.agent.internal.extension.find

internal object HttpURLIntegration {

    private const val TAG = "HttpURLIntegration"
    private const val MODULE_NAME = "httpURLConnectionTracing"

    private val defaultModuleConfiguration = HttpURLModuleConfiguration()
    private var moduleConfiguration = defaultModuleConfiguration

    init {
        Logger.d(TAG, "init()")
        AgentIntegration.registerModuleInitializationStart(MODULE_NAME)
    }

    fun attach(context: Context) {
        Logger.d(TAG, "attach()")
        AgentIntegration.obtainInstance(context).listeners += installationListener
    }

    private val configManagerListener = object : ModuleConfigurationManager.Listener {
        override fun onSetup(configurations: List<ModuleConfiguration>) {
            moduleConfiguration = configurations.find< HttpURLModuleConfiguration>() ?: defaultModuleConfiguration

            Logger.d(TAG, "onSetup(moduleConfiguration: ${moduleConfiguration})")
        }
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context, oTelInstallationContext: InstallationContext) {
            Logger.d(TAG, "onInstall()")

            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += configManagerListener

            AgentIntegration.registerModuleInitializationEnd(MODULE_NAME)

            //install HttpURLConnection auto-instrumentation if isEnabled is true
            if(moduleConfiguration.isEnabled){
                val httpUrlInstrumentation = AndroidInstrumentationLoader.getInstrumentation(HttpUrlInstrumentation::class.java)
                httpUrlInstrumentation?.install(oTelInstallationContext)
            }
        }
    }
}
