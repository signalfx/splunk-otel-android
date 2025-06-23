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

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.common.module.find
import com.splunk.rum.integration.agent.internal.legacy.LegacyNetworkMonitorModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.agent.internal.processor.SplunkInternalGlobalAttributeSpanProcessor
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.network.NetworkChangeInstrumentation
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.network.NetworkChangeListener
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_ICC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MCC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_MNC
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CARRIER_NAME
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_SUBTYPE
import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NETWORK_CONNECTION_TYPE

internal object NetworkMonitorModuleIntegration : ModuleIntegration<NetworkMonitorModuleConfiguration>(
    defaultModuleConfiguration = NetworkMonitorModuleConfiguration()
) {

    private const val TAG = "NetworkMonitorIntegration"

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

val isEnabled =
    moduleConfigurations.find<LegacyNetworkMonitorModuleConfiguration>()?.isEnabled
        ?: moduleConfiguration.isEnabled

        if (isEnabled) {
            // install Network Monitor instrumentation if isEnabled is true
            NetworkChangeInstrumentation().install(oTelInstallationContext)

            // Setup to add network attributes to all spans
            val currentNetworkProvider = Services.get(oTelInstallationContext.application).currentNetworkProvider

            val listener = NetworkChangeListener { currentNetwork ->
                SplunkInternalGlobalAttributeSpanProcessor.attributes.apply {
                    this[NETWORK_CONNECTION_TYPE] = currentNetwork.state.humanName

                    currentNetwork.subType?.let {
                        this[NETWORK_CONNECTION_SUBTYPE] = it
                    } ?: remove(NETWORK_CONNECTION_SUBTYPE)

                    currentNetwork.carrierName?.let {
                        this[NETWORK_CARRIER_NAME] = it
                    } ?: remove(NETWORK_CARRIER_NAME)

                    currentNetwork.carrierCountryCode?.let {
                        this[NETWORK_CARRIER_MCC] = it
                    } ?: remove(NETWORK_CARRIER_MCC)

                    currentNetwork.carrierNetworkCode?.let {
                        this[NETWORK_CARRIER_MNC] = it
                    } ?: remove(NETWORK_CARRIER_MNC)

                    currentNetwork.carrierIsoCountryCode?.let {
                        this[NETWORK_CARRIER_ICC] = it
                    } ?: remove(NETWORK_CARRIER_ICC)
                }
            }

            currentNetworkProvider.addNetworkChangeListener(listener)

            // Set default network.connection.type = unavailable
            // This is needed as NetworkChangeListener is not triggered on app start when no network is available
            SplunkInternalGlobalAttributeSpanProcessor.attributes[NETWORK_CONNECTION_TYPE] =
                NetworkState.NO_NETWORK_AVAILABLE.humanName
        }
    }
}
