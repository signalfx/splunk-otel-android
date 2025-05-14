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
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.network.NetworkChangeInstrumentation

internal object NetworkMonitorModuleIntegration : ModuleIntegration<NetworkMonitorModuleConfiguration>(
    defaultModuleConfiguration = NetworkMonitorModuleConfiguration()
) {

    private const val TAG = "NetworkMonitorIntegration"

    override fun onInstall(context: Context, oTelInstallationContext: InstallationContext, moduleConfigurations: List<ModuleConfiguration>) {
        Logger.d(TAG, "onInstall()")

        // install Network Monitor instrumentation if isEnabled is true
        if (moduleConfiguration.isEnabled) {
            NetworkChangeInstrumentation().install(oTelInstallationContext)
        }
    }
}
