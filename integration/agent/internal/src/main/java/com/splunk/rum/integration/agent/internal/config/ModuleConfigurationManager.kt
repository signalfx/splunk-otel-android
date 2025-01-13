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

package com.splunk.rum.integration.agent.internal.config

import com.splunk.rum.integration.agent.internal.extension.deserializeToRemoteModuleConfiguration
import com.splunk.rum.integration.agent.internal.extension.deserializeToRemoteModuleConfigurations
import com.splunk.rum.integration.agent.internal.extension.serializeToString
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.cisco.mrum.common.otel.internal.storage.OtelStorage
import com.splunk.android.common.http.HttpClient
import com.splunk.android.common.http.model.Header
import com.splunk.android.common.http.model.Response
import com.splunk.sdk.common.storage.preferences.IPreferences
import com.splunk.sdk.common.utils.extensions.forEachFast
import com.splunk.sdk.common.utils.extensions.mapNotNull
import com.splunk.sdk.common.utils.extensions.optLongNull
import com.splunk.sdk.common.utils.extensions.plusAssign
import com.splunk.sdk.common.utils.extensions.toJSONObject
import org.json.JSONException

class ModuleConfigurationManager internal constructor(
    private val preferences: IPreferences,
    private val otelStorage: OtelStorage
) {

    private val httpClient = HttpClient()

    private var serverClock: ServerClock? = null

    var remoteConfigurations: List<RemoteModuleConfiguration>
        private set

    var agentRemoteConfiguration: RemoteModuleConfiguration? = null

    var configurations: List<ModuleConfiguration> = emptyList()
        private set

    val listeners: MutableSet<Listener> = HashSet()

    init {
        remoteConfigurations = preferences.getString(MODULE_CONFIGS_KEY)?.deserializeToRemoteModuleConfigurations() ?: emptyList()
        agentRemoteConfiguration = preferences.getString(AGENT_MODULE_CONFIG_KEY)?.deserializeToRemoteModuleConfiguration()
    }

    fun currentTimeMillis(): Long {
        return serverClock?.currentTimeMillis() ?: System.currentTimeMillis()
    }

    internal fun setup(moduleConfigurations: List<ModuleConfiguration>) {
        this.configurations = moduleConfigurations
    }

    internal fun check(appName: String, agentVersion: String?, moduleNames: List<String>?) {
        val baseUrl = otelStorage.readBaseUrl() ?: return
        httpClient.makeGetRequest(
            url = "$baseUrl/eum/v1/config",
            queries = emptyList(),
            headers = createHeaders(appName, agentVersion, moduleNames),
            callback = object : HttpClient.Callback {
                override fun onSuccess(response: Response) = processResponse(response)
                override fun onFailed(e: Exception) = handleError(e)
            }
        )
    }

    internal fun publishCurrentConfigurations() {
        listeners.forEachFast { listener ->
            listener.onRemoteModuleConfigurationsChanged(this, remoteConfigurations)
            agentRemoteConfiguration?.let { listener.onAgentModuleConfigurationChanged(this, it) }
        }
    }

    private fun processResponse(response: Response) {
        if (!response.isSuccessful) {
            handleError(IllegalStateException("Wrong response code ${response.code}"))
            return
        }

        val responseJson = try {
            response.body.toString(Charsets.UTF_8).toJSONObject()
        } catch (e: Exception) {
            handleError(e)
            return
        }

        val serverTimestamp = responseJson.optLongNull("serverTimeUnixMs")
        val mrumConfig = responseJson.optJSONObject("configuration")?.optJSONObject("mrum")

        if (serverTimestamp == null || mrumConfig == null) {
            handleError(IllegalStateException("BE responded with an error"))
            return
        }

        serverClock = ServerClock(serverTimestamp, System.currentTimeMillis())
        remoteConfigurations = mrumConfig.mapNotNull { json, key ->
            try {
                RemoteModuleConfiguration(key, json.getJSONObject(key))
            } catch (e: JSONException) {
                null
            }
        }
        agentRemoteConfiguration = RemoteModuleConfiguration("mrum", mrumConfig)

        preferences.apply {
            putString(MODULE_CONFIGS_KEY, remoteConfigurations.serializeToString())
            agentRemoteConfiguration?.let {
                putString(AGENT_MODULE_CONFIG_KEY, it.serializeToString())
            }
            apply()
        }

        // TODO: not for GA
        // listeners.forEachFast { it.onRemoteModuleConfigurationsChanged(remoteModuleConfigurations) }
    }

    private fun handleError(e: Exception) {
        // TODO: not for GA
        // listeners.forEachFast { it.onRemoteModuleConfigurationsChanged(remoteModuleConfigurations) }
    }

    private fun createHeaders(appName: String, agentVersion: String?, moduleNames: List<String>?): List<Header> {
        val headers = ArrayList<Header>()

        headers += Header("x-app-type", "mrum")
        headers += Header("x-app-name", appName)
        headers += Header("x-app-platform", "android")
        headers += agentVersion?.let { Header("x-agent-version", it) }
        headers += moduleNames?.let { Header("x-agent-components", it.joinToString(",")) }

        return headers
    }

    interface Listener {
        fun onRemoteModuleConfigurationsChanged(manager: ModuleConfigurationManager, remoteConfigurations: List<RemoteModuleConfiguration>) {}

        fun onAgentModuleConfigurationChanged(manager: ModuleConfigurationManager, remoteConfiguration: RemoteModuleConfiguration) {}
    }

    private companion object {
        const val MODULE_CONFIGS_KEY = "moduleConfigs"
        const val AGENT_MODULE_CONFIG_KEY = "agentConfig"
    }
}
