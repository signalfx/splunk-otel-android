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
import com.cisco.android.common.logger.Logger
import com.cisco.android.common.logger.consumers.AndroidLogConsumer
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.splunk.rum.integration.agent.internal.config.RemoteModuleConfiguration
import com.splunk.rum.integration.agent.internal.session.SessionManager
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.cisco.mrum.common.otel.internal.storage.OtelStorage
import com.splunk.sdk.common.storage.Storage
import com.splunk.sdk.common.storage.preferences.FilePermanentCache
import com.splunk.sdk.common.storage.preferences.Preferences
import com.splunk.sdk.common.utils.extensions.forEachFast
import com.splunk.sdk.common.utils.extensions.noBackupFilesDirCompat
import com.splunk.sdk.common.utils.extensions.optLongNull
import java.io.File

class AgentIntegration private constructor(
    context: Context
) {

    private val preferences: Preferences

    private var appName: String? = null
    private var agentVersion: String? = null

    val sessionManager: SessionManager
    val moduleConfigurationManager: ModuleConfigurationManager
    val listeners: MutableSet<Listener> = HashSet()

    init {
        Logger.consumers += AndroidLogConsumer()

        registerModule(MODULE_NAME)

        val storage = Storage.attach(context)
        val otelStorage = OtelStorage.obtainInstance(storage.preferences)

        val preferencesFile = File(context.noBackupFilesDirCompat, PREFERENCES_FILE_NAME)
        preferences = Preferences(FilePermanentCache(preferencesFile))

        sessionManager = SessionManager(preferences)
        moduleConfigurationManager = ModuleConfigurationManager(preferences, otelStorage)

        // Load initial configuration to session manager.
        setModuleConfiguration(
            moduleConfigurationManager.agentRemoteConfiguration
        )

        sessionManager.sessionListeners += SessionManagerListener()
        moduleConfigurationManager.listeners += ModuleConfigurationManagerListener()
    }

    fun setup(appName: String, agentVersion: String, moduleConfigurations: List<ModuleConfiguration>): AgentIntegration {
        Logger.d(TAG, "setup(appName: $appName, agentVersion: $agentVersion, moduleConfigurations: $moduleConfigurations)")

        this.appName = appName
        this.agentVersion = agentVersion

        moduleConfigurationManager.setup(moduleConfigurations)

        return this
    }

    fun install(context: Context) {
        sessionManager.install(context)
        listeners.forEachFast { it.onInstall(context) }
    }

    private fun setModuleConfiguration(moduleConfiguration: RemoteModuleConfiguration?) {
        val config = moduleConfiguration?.config

        sessionManager.maxSessionLength = config?.optLongNull("maxSessionLength")?.times(1000L) ?: DEFAULT_SESSION_LENGTH
        sessionManager.sessionTimeout = config?.optLongNull("sessionTimeout")?.times(1000L) ?: DEFAULT_SESSION_TIMEOUT
    }

    private inner class ModuleConfigurationManagerListener : ModuleConfigurationManager.Listener {
        override fun onAgentModuleConfigurationChanged(manager: ModuleConfigurationManager, remoteConfiguration: RemoteModuleConfiguration) {
            setModuleConfiguration(remoteConfiguration)
        }
    }

    private inner class SessionManagerListener : SessionManager.SessionListener {
        override fun onSessionChanged(sessionId: String) {
            val appName = appName ?: throw IllegalStateException("Call setup() first")
            val agentVersion = agentVersion ?: throw IllegalStateException("Call setup() first")

            moduleConfigurationManager.publishCurrentConfigurations()
            moduleConfigurationManager.check(appName, agentVersion, moduleNames.toList())
        }
    }

    interface Listener {
        fun onInstall(context: Context)
    }

    companion object {

        private const val TAG = "AgentIntegration"

        private const val MODULE_NAME = "mrum"

        private const val PREFERENCES_FILE_NAME = "mrum_preferences.dat"

        private const val DEFAULT_SESSION_TIMEOUT = 900_000L
        private const val DEFAULT_SESSION_LENGTH = 60L * 60L * 1000L

        private var instanceInternal: AgentIntegration? = null
        private val moduleNames = HashSet<String>()

        val instance: AgentIntegration
            get() = instanceInternal ?: throw IllegalStateException("Instance is not created, call createInstance() first")

        fun obtainInstance(context: Context): AgentIntegration {
            if (instanceInternal == null)
                instanceInternal = AgentIntegration(context)

            return instanceInternal!!
        }

        fun registerModule(name: String) {
            moduleNames += name
        }
    }
}
