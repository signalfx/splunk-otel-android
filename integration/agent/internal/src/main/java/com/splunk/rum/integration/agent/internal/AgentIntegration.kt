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

import android.app.Application
import android.content.Context
import android.os.SystemClock
import com.cisco.android.common.utils.extensions.forEachFast
import com.splunk.rum.integration.agent.internal.model.Module
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.sdk.common.storage.AgentStorage
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionManager
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.api.OpenTelemetry

class AgentIntegration private constructor(
    context: Context
) {
    val sessionManager: ISplunkSessionManager
    val listeners: MutableSet<Listener> = HashSet()

    // The opentelemetry-android InstallationContext API needs an argument of type
    // io.opentelemetry.android.session.SessionManager. val oTelSessionManager is a no-op definition of same.
    val oTelSessionManager = object : SessionManager {
        override fun getSessionId(): String = "dummy-session-id"

        override fun addObserver(observer: SessionObserver) {
            // no-op
        }
    }

    init {
        registerModuleInitializationStart(MODULE_NAME)

        val storage = AgentStorage.attach(context)

        sessionManager = SplunkSessionManager(storage)
    }

    fun install(context: Context, openTelemetry: OpenTelemetry, moduleConfigurations: List<ModuleConfiguration>) {
        sessionManager.install(context)

        for (config in moduleConfigurations) {
            val module = modules[config.name] ?: Module(config.name)
            modules[config.name] = module.copy(configuration = config)
        }

        val oTelInstallationContext = InstallationContext(context.applicationContext as Application, openTelemetry, oTelSessionManager)
        listeners.forEachFast { it.onInstall(context, oTelInstallationContext, moduleConfigurations) }

        registerModuleInitializationEnd(MODULE_NAME)
    }

    interface Listener {
        fun onInstall(context: Context, oTelInstallationContext: InstallationContext, moduleConfigurations: List<ModuleConfiguration>)
    }

    companion object {

        private const val TAG = "AgentIntegration"

        private const val MODULE_NAME = "agent"

        private var instanceInternal: AgentIntegration? = null

        internal val modules = HashMap<String, Module>()

        val instance: AgentIntegration
            get() = instanceInternal ?: throw IllegalStateException("Instance is not created, call createInstance() first")

        fun obtainInstance(context: Context): AgentIntegration {
            if (instanceInternal == null) {
                instanceInternal = AgentIntegration(context)
            }

            return instanceInternal!!
        }

        fun registerModuleInitializationStart(name: String) {
            val module = modules[name] ?: Module(name)

            if (module.initialization != null) {
                throw IllegalStateException()
            }

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

            if (module.initialization == null) {
                throw IllegalStateException("Function registerModuleInitializationStart() for module '$name' was not called")
            }

            modules[name] = module.copy(
                initialization = module.initialization.copy(
                    endElapsed = SystemClock.elapsedRealtime()
                )
            )
        }
    }
}
