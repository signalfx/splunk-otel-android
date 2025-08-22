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
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.createZeroLengthSpan
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.instrumentation.runtime.applicationlifecycle.ApplicationLifecycleTracker
import com.splunk.rum.instrumentation.runtime.applicationlifecycle.model.ApplicationLifecycleData
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import io.opentelemetry.android.instrumentation.InstallationContext
import java.util.concurrent.TimeUnit

internal object ApplicationLifecycleModuleIntegration : ModuleIntegration<ApplicationLifecycleModuleConfiguration>(
    defaultModuleConfiguration = ApplicationLifecycleModuleConfiguration()
) {

    private const val TAG = "ApplicationLifecycle"

    private val cache: MutableList<ApplicationLifecycleData> = mutableListOf()

    override fun onAttach(context: Context) {
        ApplicationLifecycleTracker.listeners += applicationLifecycleTrackerListener
    }

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        cache.forEachFast { reportEvent(it) }
        cache.clear()
    }

    private val applicationLifecycleTrackerListener = object : ApplicationLifecycleTracker.Listener {
        override fun onApplicationLifecycleChange(applicationLifecycleData: ApplicationLifecycleData) {
            reportEvent(applicationLifecycleData)
        }
    }

    private fun reportEvent(applicationLifecycleData: ApplicationLifecycleData) {
        val provider = SplunkOpenTelemetrySdk.instance?.sdkTracerProvider ?: run {
            cache += applicationLifecycleData
            return
        }

        provider.get(RumConstants.RUM_TRACER_NAME)
            .spanBuilder(RumConstants.APP_START_NAME)
            .setAttribute(RumConstants.COMPONENT_KEY, "app-lifecycle")
            .setAttribute(RumConstants.APP_STATE_KEY, applicationLifecycleData.appState.toString())
            .createZeroLengthSpan(applicationLifecycleData.startTimestamp, TimeUnit.MILLISECONDS)
    }
}
