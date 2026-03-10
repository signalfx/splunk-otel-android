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

package com.splunk.rum.integration.crash

import android.content.Context
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.common.module.find
import com.splunk.rum.integration.agent.internal.legacy.LegacyCrashModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration

internal object CrashModuleIntegration : ModuleIntegration<CrashModuleConfiguration>(
    defaultModuleConfiguration = CrashModuleConfiguration()
) {

    private const val TAG = "CrashIntegration"

    override fun onInstall(
        context: Context,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        val isEnabled =
            moduleConfigurations.find<LegacyCrashModuleConfiguration>()?.isEnabled ?: moduleConfiguration.isEnabled

        if (isEnabled) {
            Logger.d(TAG, "Installing crash reporter")
//            val crashReporterInstrumentation = CrashReporterInstrumentation()
//            crashReporterInstrumentation.addAttributesExtractor(CrashAttributesExtractor(context))
//            crashReporterInstrumentation.install(oTelInstallationContext)
        } else {
            Logger.d(TAG, "Crash reporting is disabled")
        }
    }
}
