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

package com.splunk.rum.integration.crash.configurer

import android.annotation.SuppressLint
import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.crash.CrashReportingHandler
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager

@SuppressLint("LongLogTag")
object CrashConfigurer {

    private const val TAG = "CrashReportingConfigurer"
    private const val MODULE_NAME = "crashReporting"
    private const val DEFAULT_IS_ENABLED = true
    private lateinit var crashHandler: CrashReportingHandler

    @JvmField
    var isCrashReportingEnabled: Boolean = DEFAULT_IS_ENABLED

    init {
        Logger.d(TAG, "init()")
        AgentIntegration.registerModule(MODULE_NAME)
    }

    fun attach(context: Context) {
        Logger.d(TAG, "attach()")
        AgentIntegration.obtainInstance(context).listeners += installationListener
    }

    private val configManagerListener = object : ModuleConfigurationManager.Listener {
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context) {
            Logger.d(TAG, "onInstall()")
            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += configManagerListener

            // Registers crash handler if crash reporting enabled
            if (!CrashConfigurer::crashHandler.isInitialized) {
                crashHandler = CrashReportingHandler(context)
            }

            if (isCrashReportingEnabled) {
                crashHandler.register()
            }
        }
    }
}
