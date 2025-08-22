/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.applicationlifecycle

import android.content.Context
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import io.opentelemetry.android.instrumentation.InstallationContext

internal object ApplicationLifecycleModuleIntegration : ModuleIntegration<ApplicationLifecycleModuleConfiguration>(
    defaultModuleConfiguration = ApplicationLifecycleModuleConfiguration()
) {

    private const val TAG = "ApplicationLifecycle"

    override fun onAttach(context: Context) {

    }

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

    }

//    private fun reportEvent(startTimestamp: Long, endTimestamp: Long, name: String) {
//        val provider = SplunkOpenTelemetrySdk.instance?.sdkTracerProvider ?: run {
//            cache += StartupData(startTimestamp, endTimestamp, name)
//            return
//        }
//
//        val span = provider.get(RumConstants.RUM_TRACER_NAME)
//            .spanBuilder(RumConstants.APP_START_NAME)
//            .setStartTimestamp(startTimestamp, TimeUnit.MILLISECONDS)
//            .startSpan()
//
//        // Actual screen.name as set by SplunkInternalGlobalAttributeSpanProcessor is overwritten here to set it to
//        // "unknown" to ensure App Start event doesn't show up under a screen on UI
//        span
//            .setAttribute(RumConstants.COMPONENT_KEY, "appstart")
//            .setAttribute(RumConstants.SCREEN_NAME_KEY, RumConstants.DEFAULT_SCREEN_NAME)
//            .setAttribute("start.type", name)
//            .end(endTimestamp.toInstant())
//    }
}
