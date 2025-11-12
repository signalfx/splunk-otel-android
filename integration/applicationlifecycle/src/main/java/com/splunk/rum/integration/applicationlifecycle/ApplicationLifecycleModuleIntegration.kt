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

import android.app.Application
import android.content.Context
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.AppStateObserver
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.createZeroLengthSpan
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.applicationlifecycle.model.AppState
import com.splunk.rum.integration.applicationlifecycle.model.ApplicationLifecycleData
import io.opentelemetry.android.instrumentation.InstallationContext
import java.util.concurrent.TimeUnit

internal object ApplicationLifecycleModuleIntegration : ModuleIntegration<ApplicationLifecycleModuleConfiguration>(
    defaultModuleConfiguration = ApplicationLifecycleModuleConfiguration()
) {

    private const val TAG = "ApplicationLifecycleModuleIntegration"

    private val appStateObserver = AppStateObserver()
    private var canReport: Boolean? = null
    private val cache: MutableList<ApplicationLifecycleData> = mutableListOf()

    override fun onAttach(context: Context) {
        Logger.d(TAG, "onAttach() called")
        appStateObserver.listener = appStateListener
        appStateObserver.attach(context as Application)
    }

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        if (moduleConfiguration.isEnabled) {
            Logger.d(TAG, "Module is enabled. Reporting events.")
            canReport = true
            cache.forEachFast { reportEvent(it) }
        } else {
            Logger.w(TAG, "Module is disabled.")
            canReport = false
        }

        cache.clear()
    }

    private val appStateListener = object : AppStateObserver.Listener {

        override fun onAppStarted() {
            reportEvent(ApplicationLifecycleData(System.currentTimeMillis(), AppState.CREATED))
        }

        override fun onAppForegrounded() {
            reportEvent(ApplicationLifecycleData(System.currentTimeMillis(), AppState.FOREGROUND))
        }

        override fun onAppBackgrounded() {
            reportEvent(ApplicationLifecycleData(System.currentTimeMillis(), AppState.BACKGROUND))
        }
    }

    private fun reportEvent(applicationLifecycleData: ApplicationLifecycleData) {
        if (canReport == false) {
            Logger.i(TAG, "Cannot report event, module disabled.")
            return
        }

        val logger = SplunkOpenTelemetrySdk.instance?.sdkLoggerProvider

        if (logger == null || canReport == null) {
            Logger.i(TAG, "Tracer provider not ready or reporting status unknown. Caching event")
            cache += applicationLifecycleData
            return
        }

        Logger.d(TAG, "Creating log for app lifecycle event: $applicationLifecycleData")
        logger.get(RumConstants.RUM_TRACER_NAME)
            .logRecordBuilder()
            .setTimestamp(applicationLifecycleData.timestamp, TimeUnit.MILLISECONDS)
            .setAttribute(RumConstants.LOG_EVENT_NAME_KEY, RumConstants.APP_LIFECYCLE_NAME)
            .setAttribute(RumConstants.COMPONENT_KEY, RumConstants.APP_LIFECYCLE_COMPONENT)
            .setAttribute(RumConstants.APP_STATE_KEY, applicationLifecycleData.appState.attributeValue)
            .emit()
    }

    private val AppState.attributeValue: String
        get() = when (this) {
            AppState.CREATED -> "created"
            AppState.FOREGROUND -> "foreground"
            AppState.BACKGROUND -> "background"
        }
}
