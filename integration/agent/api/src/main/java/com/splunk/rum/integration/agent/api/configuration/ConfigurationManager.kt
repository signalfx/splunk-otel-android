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

package com.splunk.rum.integration.agent.api.configuration

import android.content.Context
import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.storage.IAgentStorage
import com.splunk.rum.integration.agent.api.AgentConfiguration

internal class ConfigurationManager private constructor(private val agentStorage: IAgentStorage) {
    fun preProcessConfiguration(context: Context, proposalConfig: AgentConfiguration): AgentConfiguration {
        val config = if (proposalConfig.appVersion == null) {
            proposalConfig.copy(appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName)
        } else {
            proposalConfig
        }

        if (config.endpoint == null) {
            Logger.w(
                TAG,
                "Agent installed without endpoint configuration. " +
                    "Data will be buffered but not sent until you configure an endpoint."
            )
            agentStorage.deleteTracesBaseUrl()
            agentStorage.deleteLogsBaseUrl()
        } else {
            val endpoint = config.endpoint
            agentStorage.writeTracesBaseUrl(endpoint.traceEndpoint.toExternalForm())
            endpoint.sessionReplayEndpoint?.let { logsUrl ->
                agentStorage.writeLogsBaseUrl(logsUrl.toExternalForm())
                Logger.d(TAG, "Trace and session replay endpoint URLs written to storage")
            } ?: run {
                agentStorage.deleteLogsBaseUrl()
                Logger.d(TAG, "Trace endpoint URL written to storage, session replay endpoint not configured")
            }
        }

        Logger.d(TAG, "preProcessConfiguration() proposalConfig: $proposalConfig, config: $config")

        return config
    }

    companion object {
        private const val TAG = "ConfigurationManager"
        private var instanceInternal: ConfigurationManager? = null
        fun obtainInstance(agentStorage: IAgentStorage): ConfigurationManager {
            if (instanceInternal == null) {
                instanceInternal = ConfigurationManager(agentStorage)
            }

            return requireNotNull(instanceInternal)
        }
    }
}
