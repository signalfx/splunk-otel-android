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

package com.splunk.rum.integration.navigation

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.splunk.rum.integration.agent.internal.extension.find
import com.splunk.rum.integration.agent.internal.span.SplunkInternalGlobalAttributeSpanProcessor
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.sdk.common.otel.SplunkOpenTelemetrySdk
import com.splunk.sdk.common.otel.internal.RumConstants
import io.opentelemetry.android.instrumentation.InstallationContext

internal object NavigationIntegration {

    private const val TAG = "NavigationIntegration"
    private const val MODULE_NAME = "navigation"

    private val defaultModuleConfiguration = NavigationModuleConfiguration()

    private var moduleConfiguration = defaultModuleConfiguration

    init {
        AgentIntegration.registerModuleInitializationStart(MODULE_NAME)
    }

    fun attach(context: Context) {
        AgentIntegration.obtainInstance(context).listeners += installationListener

        Navigation.listener = navigationListener
    }

    private val navigationListener = object : Navigation.Listener {
        override fun onScreenNameChanged(screenName: String) {
            if (!moduleConfiguration.isEnabled)
                return

            Logger.d(TAG, "onScreenNameChanged(screenName: $screenName)")

            val provider = SplunkOpenTelemetrySdk.instance?.sdkTracerProvider ?: return

            SplunkInternalGlobalAttributeSpanProcessor.attributes.removeIf { it.name == "screen.name" }
            SplunkInternalGlobalAttributeSpanProcessor.attributes += SplunkInternalGlobalAttributeSpanProcessor.Attribute.String("screen.name", screenName)

            provider.get(RumConstants.RUM_TRACER_NAME)
                .spanBuilder("Created")
                .setAttribute("component", "ui")
                .startSpan()
                .end()
        }
    }

    private val configManagerListener = object : ModuleConfigurationManager.Listener {
        override fun onSetup(configurations: List<ModuleConfiguration>) {
            moduleConfiguration = configurations.find<NavigationModuleConfiguration>() ?: defaultModuleConfiguration

            Logger.d(TAG, "onSetup(moduleConfiguration: $moduleConfiguration)")
        }
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context, oTelInstallationContext: InstallationContext) {
            Logger.d(TAG, "onInstall()")

            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += configManagerListener

            AgentIntegration.registerModuleInitializationEnd(MODULE_NAME)
        }
    }
}
