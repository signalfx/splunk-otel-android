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

package com.cisco.android.rum.integration.agent.api.configuration

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.cisco.android.rum.integration.agent.api.AgentConfiguration
import com.cisco.mrum.common.otel.internal.storage.OtelStorage

internal class ConfigurationManager private constructor(
    private val otelStorage: OtelStorage
) {
    fun preProcessConfiguration(context: Context, proposalConfig: AgentConfiguration): AgentConfiguration {
        val config = proposalConfig.copy()

        if (config.appName == null)
            config.appName = context.packageName

        if (config.appVersion == null)
            config.appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

        otelStorage.writeBaseUrl(config.url.toExternalForm())

        Logger.d(TAG, "preProcessConfiguration() proposalConfig: $proposalConfig, config: $config")

        return config
    }

    companion object {
        private const val TAG = "ConfigurationManager"
        private var instanceInternal: ConfigurationManager? = null
        fun obtainInstance(urlStorage: OtelStorage): ConfigurationManager {
            if (instanceInternal == null)
                instanceInternal = ConfigurationManager(urlStorage)

            return requireNotNull(instanceInternal)
        }
    }
}
