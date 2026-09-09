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

import android.app.Application
import android.content.Context
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes

abstract class ModuleIntegration<T : ModuleConfiguration>(protected val defaultModuleConfiguration: T) {

    protected var moduleConfiguration: T = defaultModuleConfiguration
        private set

    protected var globalAttributes: Attributes = Attributes.empty()
        private set

    protected lateinit var sessionManager: ISplunkSessionManager
        private set

    protected open val observesSessionChanges: Boolean = false

    fun attach(context: Context) {
        val agentIntegration = AgentIntegration.obtainInstance(context)
        agentIntegration.listeners += installationListener

        onAttach(context)
    }

    protected open fun onAttach(context: Context) {}

    protected abstract fun onInstall(
        application: Application,
        openTelemetry: OpenTelemetry,
        moduleConfigurations: List<ModuleConfiguration>
    )

    protected open fun onPostInstall() {
    }

    protected open fun onSessionChange(sessionId: String) {
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onSessionManagerReady(sessionManager: ISplunkSessionManager) {
            this@ModuleIntegration.sessionManager = sessionManager
            if (observesSessionChanges) {
                sessionManager.sessionListeners += sessionChangeListener
            }
        }

        override fun onInstall(
            application: Application,
            openTelemetry: OpenTelemetry,
            moduleConfigurations: List<ModuleConfiguration>
        ) {
            val clazz = defaultModuleConfiguration::class

            moduleConfiguration = moduleConfigurations.find { it::class == clazz } as? T ?: defaultModuleConfiguration
            this@ModuleIntegration.globalAttributes = AgentIntegration.obtainInstance(application).globalAttributes
            AgentIntegration.registerModuleInitializationStart(defaultModuleConfiguration.name)
            this@ModuleIntegration.onInstall(application, openTelemetry, moduleConfigurations)
            AgentIntegration.registerModuleInitializationEnd(defaultModuleConfiguration.name)
        }

        override fun onPostInstall() {
            this@ModuleIntegration.onPostInstall()
        }
    }

    private val sessionChangeListener = object : SplunkSessionManager.SessionListener {
        override fun onSessionChanged(sessionId: String, timestamp: Long) {
            onSessionChange(sessionId)
        }
    }
}
