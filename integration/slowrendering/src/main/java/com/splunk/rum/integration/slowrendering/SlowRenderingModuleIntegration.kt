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

package com.splunk.rum.integration.slowrendering

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.common.module.find
import com.splunk.rum.integration.agent.internal.legacy.LegacySlowRenderingModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.slowrendering.SlowRenderingInstrumentation

internal object SlowRenderingModuleIntegration : ModuleIntegration<SlowRenderingModuleConfiguration>(
    defaultModuleConfiguration = SlowRenderingModuleConfiguration()
) {

    private const val TAG = "SlowRendering"

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        val isEnabled =
            moduleConfigurations.find<LegacySlowRenderingModuleConfiguration>()?.isEnabled
                ?: moduleConfiguration.isEnabled
        val interval =
            moduleConfigurations.find<LegacySlowRenderingModuleConfiguration>()?.interval
                ?: moduleConfiguration.interval

        if (isEnabled) {
            Logger.d(TAG, "Installing Slow Rendering Detector")
            val slowRenderingInstrumentation = SlowRenderingInstrumentation()
            slowRenderingInstrumentation.setSlowRenderingDetectionPollInterval(interval)
            slowRenderingInstrumentation.install(oTelInstallationContext)
        } else {
            Logger.d(TAG, "Slow Rendering detection is disabled")
        }
    }
}
