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

package com.splunk.rum.integration.networkmonitor

import android.app.Application
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.instrumentation.networkmonitor.internal.NetworkMonitorInstrumentation
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.common.module.find
import com.splunk.rum.integration.agent.internal.legacy.LegacyNetworkMonitorModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.agent.internal.processor.SplunkInternalGlobalAttributeSpanProcessor
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NetworkConnectionTypeIncubatingValues.UNKNOWN

internal object NetworkMonitorModuleIntegration : ModuleIntegration<NetworkMonitorModuleConfiguration>(
    defaultModuleConfiguration = NetworkMonitorModuleConfiguration()
) {

    private const val TAG = "NetworkMonitorIntegration"

    override fun onInstall(
        application: Application,
        openTelemetry: OpenTelemetry,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        val isEnabled =
            moduleConfigurations.find<LegacyNetworkMonitorModuleConfiguration>()?.isEnabled
                ?: moduleConfiguration.isEnabled

        if (isEnabled) {
            // Seed an indeterminate state before callbacks can update it with a detected network.
            NetworkGlobalAttributesUpdater.update(
                SplunkInternalGlobalAttributeSpanProcessor.attributes,
                Attributes.of(NETWORK_CONNECTION_TYPE, UNKNOWN)
            )
            NetworkMonitorInstrumentation().apply {
                addNetworkChangeListener { attributes ->
                    NetworkGlobalAttributesUpdater.update(
                        SplunkInternalGlobalAttributeSpanProcessor.attributes,
                        attributes
                    )
                }
                install(application, openTelemetry)
            }
        }
    }
}
