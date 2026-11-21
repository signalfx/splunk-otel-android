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
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants.SESSION_ID_KEY
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.common.utils.AppStateObserver
import com.splunk.rum.common.utils.extensions.forEachFast
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.applicationlifecycle.model.AppState
import com.splunk.rum.integration.applicationlifecycle.model.ApplicationLifecycleData
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import java.util.concurrent.TimeUnit

internal object ApplicationLifecycleModuleIntegration : ModuleIntegration<ApplicationLifecycleModuleConfiguration>(
    defaultModuleConfiguration = ApplicationLifecycleModuleConfiguration()
) {

    private const val TAG = "ApplicationLifecycleModuleIntegration"
    override val observesSessionChanges = true

    private var canReport: Boolean? = null
    private val preInstallEvents = PreInstallLifecycleCache()
    private val pendingEvents = StartupLifecycleContext()

    override fun onAttach(context: Context) {
        Logger.d(TAG, "onAttach() called")
        registerAppStateListener()
        AppStateObserver.attach(context as Application)
    }

    override fun onInstall(
        application: Application,
        openTelemetry: OpenTelemetry,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        if (moduleConfiguration.isEnabled) {
            Logger.d(TAG, "Module is enabled. Reporting events.")
            canReport = true
            // A previous skipped install may have removed the listener.
            registerAppStateListener()
            preInstallEvents.eventsToReport(sessionManager.allowBackgroundSessionCreation).forEachFast(::reportEvent)
        } else {
            disableAndRemoveListener()
        }

        preInstallEvents.clear()
    }

    private fun registerAppStateListener() {
        if (appStateListener !in AppStateObserver.listeners) {
            AppStateObserver.listeners += appStateListener
        }
    }

    public override fun onInstallSkipped() {
        Logger.d(TAG, "Install skipped. Removing AppState listener.")
        removeListenerAndClearCache()
    }

    internal fun disableAndRemoveListener() {
        Logger.w(TAG, "Module is disabled. Removing AppState listener.")
        removeListenerAndClearCache()
    }

    private fun removeListenerAndClearCache() {
        canReport = false
        AppStateObserver.listeners.removeAll { it === appStateListener }
        preInstallEvents.clear()
        pendingEvents.clear()
    }

    internal val appStateListenerReference: AppStateObserver.Listener get() = appStateListener

    override fun onSessionChange(sessionId: String) {
        if (canReport == true && !sessionManager.allowBackgroundSessionCreation) emitPendingEvents(sessionId)
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

        override fun onAppClosed() {
            preInstallEvents.markAppClosed()
            pendingEvents.clear()
        }
    }

    internal fun reportEvent(applicationLifecycleData: ApplicationLifecycleData) {
        if (canReport == false) return

        val logger = SplunkOpenTelemetrySdk.instance?.sdkLoggerProvider

        if (logger == null || canReport == null) {
            preInstallEvents.add(applicationLifecycleData)
            return
        }

        if (sessionManager.allowBackgroundSessionCreation) {
            emitEvent(applicationLifecycleData, logger, null)
            return
        }

        val sessionId = sessionManager.sessionId
        if (sessionId == null) {
            pendingEvents.hold(applicationLifecycleData)
            // The session manager may have created a session between our read and this write.
            sessionManager.sessionId?.let(::emitPendingEvents)
            return
        }

        emitPendingEvents(sessionId)
        emitEvent(applicationLifecycleData, logger, sessionId)
    }

    private fun emitPendingEvents(sessionId: String) {
        val logger = SplunkOpenTelemetrySdk.instance?.sdkLoggerProvider ?: return
        pendingEvents.take().forEachFast { emitEvent(it, logger, sessionId) }
    }

    private fun emitEvent(
        applicationLifecycleData: ApplicationLifecycleData,
        logger: SdkLoggerProvider,
        sessionId: String?
    ) {
        Logger.d(TAG) { "Creating log for app lifecycle event: $applicationLifecycleData" }
        val builder = logger.get(GlobalRumConstants.RUM_TRACER_NAME)
            .logRecordBuilder()
            .setTimestamp(applicationLifecycleData.timestamp, TimeUnit.MILLISECONDS)
            .setAttribute(GlobalRumConstants.LOG_EVENT_NAME_KEY, RumConstants.APP_LIFECYCLE_LOG_NAME)
            .setAttribute(GlobalRumConstants.COMPONENT_KEY, RumConstants.COMPONENT_APP_LIFECYCLE)
            .setAttribute(GlobalRumConstants.APP_STATE_KEY, applicationLifecycleData.appState.attributeValue)
        sessionId?.let { builder.setAttribute(SESSION_ID_KEY, it) }
        builder.emit()
    }

    private val AppState.attributeValue: String
        get() = when (this) {
            AppState.CREATED -> GlobalRumConstants.APP_STATE_CREATED
            AppState.FOREGROUND -> GlobalRumConstants.APP_STATE_FOREGROUND
            AppState.BACKGROUND -> GlobalRumConstants.APP_STATE_BACKGROUND
        }
}
