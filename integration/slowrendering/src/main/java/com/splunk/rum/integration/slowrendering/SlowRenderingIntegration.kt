/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.slowrendering

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.extension.find
import com.splunk.rum.integration.agent.internal.legacy.LegacySlowRenderingModuleConfiguration
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation

@SuppressLint("LongLogTag")
internal object SlowRenderingIntegration {

    private const val TAG = "SlowRenderingIntegration"
    private const val MODULE_NAME = "slowrendering"

    private val defaultModuleConfiguration = SlowRenderingModuleConfiguration()

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
            moduleConfigurations: List<ModuleConfiguration>
        ) {
            Logger.d(TAG, "onInstall()")

            val isEnabled = moduleConfigurations.find<LegacySlowRenderingModuleConfiguration>()?.isEnabled
                ?: moduleConfigurations.find<SlowRenderingModuleConfiguration>()?.isEnabled
                ?: defaultModuleConfiguration.isEnabled

            AgentIntegration.registerModuleInitializationEnd(MODULE_NAME)

            if (isEnabled) {
                Logger.d(TAG, "Installing Slow Rendering Detector")
                Log.e("TONY", "HERE")
                val slowRenderingInstrumentation = SlowRenderingInstrumentation()
                slowRenderingInstrumentation.install(oTelInstallationContext)
                Log.e("TONY", "After installing slow rendering")
            } else {
                Log.e("TONY", "HERE!!!!")

                Logger.d(TAG, "Slow Rendering reporting is disabled")
            }
        }
    }
}