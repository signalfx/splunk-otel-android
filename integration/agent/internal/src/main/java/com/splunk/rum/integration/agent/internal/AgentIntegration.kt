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

package com.splunk.rum.integration.agent.internal

import android.content.Context
import android.os.SystemClock
import com.cisco.android.common.logger.Logger
import com.cisco.android.common.utils.extensions.forEachFast
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.splunk.rum.integration.agent.internal.model.Module
import com.splunk.rum.integration.agent.internal.session.SessionManager
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.sdk.common.storage.AgentStorage

class AgentIntegration private constructor(
    context: Context
) {
    private var appName: String? = null
    private var agentVersion: String? = null

    private var startTimestamp = 0L
    private var startElapsed = 0L

    val sessionManager: SessionManager
    val moduleConfigurationManager: ModuleConfigurationManager
    val listeners: MutableSet<Listener> = HashSet()

    init {
        startTimestamp = System.currentTimeMillis()
        startElapsed = SystemClock.elapsedRealtime()

        registerModuleInitializationStart(MODULE_NAME)

        val storage = AgentStorage.attach(context)

        sessionManager = SessionManager(storage)
        moduleConfigurationManager = ModuleConfigurationManager(storage)

        sessionManager.sessionListeners += SessionManagerListener()
    }

    fun setup(appName: String, agentVersion: String, moduleConfigurations: List<ModuleConfiguration>): AgentIntegration {
        Logger.d(TAG, "setup(appName: $appName, agentVersion: $agentVersion, moduleConfigurations: $moduleConfigurations)")

        this.appName = appName
        this.agentVersion = agentVersion

        for (config in moduleConfigurations) {
            val module = modules[config.name] ?: Module(config.name)
            modules[config.name] = module.copy(configuration = config)
        }

        moduleConfigurationManager.setup(moduleConfigurations)

        return this
    }

    fun install(context: Context) {
        sessionManager.install(context)
        listeners.forEachFast { it.onInstall(context) }

        registerModuleInitializationEnd(MODULE_NAME)
    }


    private inner class SessionManagerListener : SessionManager.SessionListener {
        override fun onSessionChanged(sessionId: String) {
            val appName = appName ?: throw IllegalStateException("Call setup() first")
            val agentVersion = agentVersion ?: throw IllegalStateException("Call setup() first")
        }
    }

    interface Listener {
        fun onInstall(context: Context)
    }

    companion object {

        private const val TAG = "AgentIntegration"

        private const val MODULE_NAME = "agent"

        private var instanceInternal: AgentIntegration? = null

        internal val modules = HashMap<String, Module>()

        val instance: AgentIntegration
            get() = instanceInternal ?: throw IllegalStateException("Instance is not created, call createInstance() first")

        fun obtainInstance(context: Context): AgentIntegration {
            if (instanceInternal == null)
                instanceInternal = AgentIntegration(context)

            return instanceInternal!!
        }

        fun registerModuleInitializationStart(name: String) {
            val module = modules[name] ?: Module(name)

            if (module.initialization != null)
                throw IllegalStateException()

            modules[name] = module.copy(
                initialization = Module.Initialization(
                    startTimestamp = System.currentTimeMillis(),
                    startElapsed = SystemClock.elapsedRealtime(),
                    endElapsed = null
                )
            )
        }

        fun registerModuleInitializationEnd(name: String) {
            val module = modules[name] ?: throw IllegalStateException("Initialization start for module '$name' was not called")

            if (module.initialization == null)
                throw IllegalStateException("Function registerModuleInitializationStart() for module '$name' was not called")

            modules[name] = module.copy(
                initialization = module.initialization.copy(
                    endElapsed = SystemClock.elapsedRealtime()
                )
            )
        }
    }
}
