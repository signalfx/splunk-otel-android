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

package com.splunk.rum.integration.anr

import android.content.Context
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.common.module.find
import com.splunk.rum.integration.agent.internal.legacy.LegacyAnrModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.anr.AnrInstrumentation

internal object AnrModuleIntegration : ModuleIntegration<AnrModuleConfiguration>(
    defaultModuleConfiguration = AnrModuleConfiguration()
) {

    private const val TAG = "AnrIntegration"

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        val isEnabled =
            moduleConfigurations.find<LegacyAnrModuleConfiguration>()?.isEnabled ?: moduleConfiguration.isEnabled

        if (isEnabled) {
            Logger.d(TAG, "Installing ANR reporter")
            val anrDetectorInstrumentation = AnrInstrumentation()
            anrDetectorInstrumentation.addAttributesExtractor(AnrAttributesExtractor())
            anrDetectorInstrumentation.install(oTelInstallationContext)
        } else {
            Logger.d(TAG, "ANR reporting is disabled")
        }
    }
}
