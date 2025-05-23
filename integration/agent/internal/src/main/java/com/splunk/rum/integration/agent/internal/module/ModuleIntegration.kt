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

package com.splunk.rum.integration.agent.internal.module

import android.content.Context
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.android.instrumentation.InstallationContext

abstract class ModuleIntegration<T : ModuleConfiguration>(protected val defaultModuleConfiguration: T) {

    protected var moduleConfiguration: T = defaultModuleConfiguration
        private set

    init {
        AgentIntegration.registerModuleInitializationStart(defaultModuleConfiguration.name)
    }

    fun attach(context: Context) {
        AgentIntegration.obtainInstance(context).listeners += installationListener

        onAttach(context)
    }

    protected open fun onAttach(context: Context) {}

    protected open fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(
            context: Context,
            oTelInstallationContext: InstallationContext,
            moduleConfigurations: List<ModuleConfiguration>
        ) {
            val clazz = defaultModuleConfiguration::class

            moduleConfiguration = moduleConfigurations.find { it::class == clazz } as? T ?: defaultModuleConfiguration
            this@ModuleIntegration.onInstall(context, oTelInstallationContext, moduleConfigurations)

            AgentIntegration.registerModuleInitializationEnd(defaultModuleConfiguration.name)
        }
    }
}
