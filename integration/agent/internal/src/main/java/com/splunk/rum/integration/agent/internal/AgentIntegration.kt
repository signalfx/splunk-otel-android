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
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.common.otel.internal.RumConstants.RUM_TRACER_NAME
import com.splunk.rum.common.storage.AgentStorage
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.attributes.AttributeConstants.PREVIOUS_SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.attributes.AttributeConstants.SESSION_ID_KEY
import com.splunk.rum.integration.agent.internal.model.Module
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.session.SplunkSessionManager
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionManager
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.sdk.OpenTelemetrySdk
import java.util.concurrent.TimeUnit

class AgentIntegration private constructor(context: Context) {
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
        val storage = AgentStorage.attach(context)

        sessionManager = SplunkSessionManager(storage)
    }

    fun install(context: Context, openTelemetry: OpenTelemetrySdk, moduleConfigurations: List<ModuleConfiguration>) {
        registerModuleInitializationStart(MODULE_NAME)

        sessionManager.sessionListeners += object : SplunkSessionManager.SessionListener {
            override fun onSessionChanged(sessionId: String, timestamp: Long) {
                openTelemetry.sdkLoggerProvider.get(RUM_TRACER_NAME)
                    .logRecordBuilder()
                    .setAttribute(RumConstants.LOG_EVENT_NAME_KEY, "session.start")
                    .setTimestamp(timestamp, TimeUnit.MILLISECONDS)
                    .setAttribute(SESSION_ID_KEY, sessionManager.sessionId)
                    .setAttribute(PREVIOUS_SESSION_ID_KEY, sessionManager.previousSessionId)
                    .emit()
            }
        }
        sessionManager.install(context)

        val seenConfigurationNames = mutableSetOf<String>()

        for (config in moduleConfigurations) {
            if (!seenConfigurationNames.add(config.name)) {
                Logger.w(TAG, "Duplicate module configuration: ${config.javaClass.name} was ignored.")
                continue
            }
            val module = modules[config.name] ?: Module(config.name)
            modules[config.name] = module.copy(configuration = config)
        }

        // Separate modules into background-safe and main-thread-required
        val (mainThreadConfigs, backgroundConfigs) = moduleConfigurations.partition { it.requiresMainThread }

        Logger.d(TAG, "Background-safe modules: ${backgroundConfigs.map { it.name }}")
        Logger.d(TAG, "Main-thread-required modules: ${mainThreadConfigs.map { it.name }}")

        val oTelInstallationContext =
            InstallationContext(context.applicationContext as Application, openTelemetry, oTelSessionManager)

        // Install background-safe modules immediately
        if (backgroundConfigs.isNotEmpty()) {
            listeners.forEachFast { it.onInstall(context, oTelInstallationContext, backgroundConfigs) }
        }

        // Post main-thread-required modules to the main handler
        if (mainThreadConfigs.isNotEmpty()) {
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                Logger.d(TAG, "Installing main-thread-required modules on main thread")
                listeners.forEachFast { it.onInstall(context, oTelInstallationContext, mainThreadConfigs) }

                // Call onPostInstall after main thread modules are initialized
                listeners.forEachFast { it.onPostInstall() }
            }
        } else {
            // No main thread modules, call onPostInstall immediately
            listeners.forEachFast { it.onPostInstall() }
        }

        registerModuleInitializationEnd(MODULE_NAME)
    }

    interface Listener {
        fun onInstall(
            context: Context,
            oTelInstallationContext: InstallationContext,
            moduleConfigurations: List<ModuleConfiguration>
        )
        fun onPostInstall()
    }

    companion object {

        private const val TAG = "AgentIntegration"

        private const val MODULE_NAME = "agent"

        private var instanceInternal: AgentIntegration? = null

        val modules = HashMap<String, Module>()

        var lowestApiLevel: Int = Constants.LOWEST_RUNTIME_API_LEVEL

        val instance: AgentIntegration
            get() = instanceInternal
                ?: throw IllegalStateException("Instance is not created, call createInstance() first")

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

            Logger.d(
                TAG,
                "registerModuleInitializationStart() module: $name,  startTimestamp:${modules[name]?.initialization?.startTimestamp}, startElapsed:${modules[name]?.initialization?.startElapsed}"
            )
        }

        fun registerModuleInitializationEnd(name: String) {
            val module =
                modules[name] ?: throw IllegalStateException("Initialization start for module '$name' was not called")

            if (module.initialization == null) {
                throw IllegalStateException(
                    "Function registerModuleInitializationStart() for module '$name' was not called"
                )
            }

            modules[name] = module.copy(
                initialization = module.initialization.copy(
                    endElapsed = SystemClock.elapsedRealtime()
                )
            )

            Logger.d(
                TAG,
                "registerModuleInitializationEnd() module: $name,  startTimestamp:${modules[name]?.initialization?.startTimestamp}, startElapsed:${modules[name]?.initialization?.startElapsed}, endElapsed:${modules[name]?.initialization?.endElapsed}"
            )
        }
    }
}
