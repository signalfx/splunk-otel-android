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

package com.splunk.rum.integration.startup

import android.app.Application
import android.content.Context
import com.splunk.rum.agent.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.agent.common.otel.extensions.toInstant
import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.common.utils.extensions.forEachFast
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.common.module.toSplunkString
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.AgentIntegration.Companion.modules
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.startup.model.StartupData
import com.splunk.rum.startup.ApplicationStartupTimekeeper
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context as OtelContext
import io.opentelemetry.sdk.trace.SdkTracerProvider
import java.util.concurrent.TimeUnit

internal object StartupModuleIntegration : ModuleIntegration<StartupModuleConfiguration>(
    defaultModuleConfiguration = StartupModuleConfiguration()
) {

    private const val TAG = "StartupIntegration"

    private val lock = Any()

    private val cache: MutableList<StartupData> = mutableListOf()

    @Volatile
    private var isInitializationReported = false

    @Volatile
    private var isInstallComplete = false

    override fun onAttach(context: Context) {
        Logger.d(TAG, "onAttach() - adding listener to ApplicationStartupTimekeeper")
        ApplicationStartupTimekeeper.listeners += applicationStartupTimekeeperListener
    }

    override fun onInstall(
        application: Application,
        openTelemetry: OpenTelemetry,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")
    }

    override fun onPostInstall() {
        super.onPostInstall()
        Logger.d(TAG, "onPostInstall()")

        val cachedEvents: List<StartupData>

        synchronized(lock) {
            cachedEvents = cache.toList()
            cache.clear()
            isInstallComplete = true
        }

        if (cachedEvents.isNotEmpty()) {
            Logger.d(TAG) { "Processing deferred cache (size: ${cachedEvents.size})" }
            cachedEvents.forEachFast {
                Logger.d(TAG) { "Processing cached event: ${it.name}" }
                reportEventInternal(it.startTimestamp, it.endTimestamp, it.name)
            }
        }

        Logger.d(TAG, "onPostInstall() complete")
    }

    private val applicationStartupTimekeeperListener = object : ApplicationStartupTimekeeper.Listener {
        override fun onColdStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(TAG) {
                "onColdStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)"
            }
            reportEvent(startTimestamp, endTimestamp, RumConstants.APP_START_TYPE_COLD)
        }

        override fun onWarmStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(TAG) {
                "onWarmStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)"
            }
            reportEvent(startTimestamp, endTimestamp, RumConstants.APP_START_TYPE_WARM)
        }

        override fun onHotStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(TAG) {
                "onHotStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)"
            }
            reportEvent(startTimestamp, endTimestamp, RumConstants.APP_START_TYPE_HOT)
        }
    }

    private fun reportEvent(startTimestamp: Long, endTimestamp: Long, name: String) {
        synchronized(lock) {
            if (!isInstallComplete) {
                Logger.d(TAG) { "reportEvent() - install not complete, caching event: $name" }
                cache += StartupData(startTimestamp, endTimestamp, name)
                return
            }
        }

        // Install is complete, process the event immediately
        reportEventInternal(startTimestamp, endTimestamp, name)
    }

    private fun reportEventInternal(startTimestamp: Long, endTimestamp: Long, name: String) {
        val provider = SplunkOpenTelemetrySdk.instance?.sdkTracerProvider
        if (provider == null) {
            Logger.e(TAG, "reportEventInternal() - SDK not ready")
            return
        }

        Logger.d(TAG) { "reportEventInternal() - SDK ready, creating span for: $name" }

        val shouldReportInitialization = synchronized(lock) {
            if (isInitializationReported) {
                false
            } else {
                isInitializationReported = true
                true
            }
        }

        val span = provider.get(GlobalRumConstants.RUM_TRACER_NAME)
            .spanBuilder(RumConstants.APP_START_SPAN_NAME)
            .setStartTimestamp(startTimestamp, TimeUnit.MILLISECONDS)
            .startSpan()

        if (shouldReportInitialization) {
            reportInstallSpan(span, provider)
        }

        // Actual screen.name as set by SplunkInternalGlobalAttributeSpanProcessor is overwritten here to set it to
        // "unknown" to ensure App Start event doesn't show up under a screen on UI
        span
            .setAttribute(GlobalRumConstants.COMPONENT_KEY, RumConstants.COMPONENT_APP_START)
            .setAttribute(GlobalRumConstants.SCREEN_NAME_KEY, GlobalRumConstants.DEFAULT_SCREEN_NAME)
            .setAttribute(RumConstants.APP_START_TYPE_KEY, name)
            .end(endTimestamp.toInstant())

        Logger.d(TAG) { "reportEventInternal() - span sent successfully for: $name" }
    }

    private fun reportInstallSpan(appStartSpan: Span, provider: SdkTracerProvider) {
        val modules = modules.values

        val firstInitialization =
            modules.minByOrNull { it.initialization?.startTimestamp ?: Long.MAX_VALUE }?.initialization
                ?: throw IllegalStateException("Module initialization did not started")
        val lastInitialization =
            modules.maxByOrNull { it.initialization?.endElapsed ?: Long.MIN_VALUE }?.initialization
                ?: throw IllegalStateException("Module initialization did not complete")

        val storedStart = AgentIntegration.installStartTimestamp
        val storedStartElapsed = AgentIntegration.installStartElapsed
        val storedEnd = AgentIntegration.installEndElapsed

        val installStartTimestamp: Long
        val installStartElapsed: Long
        val installEndElapsed: Long

        if (storedStart != null && storedStartElapsed != null && storedEnd != null) {
            installStartTimestamp = storedStart
            installStartElapsed = storedStartElapsed
            installEndElapsed = storedEnd
        } else {
            installStartTimestamp = firstInitialization.startTimestamp
            installStartElapsed = firstInitialization.startElapsed
            installEndElapsed = lastInitialization.endElapsed!!
        }

        val installEndTimestamp = installStartTimestamp + (installEndElapsed - installStartElapsed)

        Logger.d(TAG) {
            "reportInstallSpan() startTimestamp: $installStartTimestamp, " +
                "endTimestamp: $installEndTimestamp, duration: ${installEndTimestamp - installStartTimestamp}ms"
        }

        val installSpan = provider.get(GlobalRumConstants.RUM_TRACER_NAME)
            .spanBuilder(RumConstants.APP_START_INSTALL_SPAN_NAME)
            .setParent(OtelContext.current().with(appStartSpan))
            .setStartTimestamp(installStartTimestamp, TimeUnit.MILLISECONDS)
            .startSpan()

        installSpan.setAttribute(GlobalRumConstants.COMPONENT_KEY, RumConstants.COMPONENT_APP_START)
            .setAttribute(GlobalRumConstants.SCREEN_NAME_KEY, GlobalRumConstants.DEFAULT_SCREEN_NAME)

        val resources = modules.joinToString(",", "[", "]") {
            it.configuration?.toSplunkString()
                ?: "${it.name}.enabled:true"
        }

        installSpan.setAttribute(RumConstants.APP_START_CONFIG_SETTINGS_KEY, resources)

        for (module in modules) {
            if (module.initialization == null) {
                throw IllegalStateException("Module '${module.name}' initialization has not been started")
            }

            if (module.initialization!!.endElapsed == null) {
                throw IllegalStateException("Module '${module.name}' is not initialized")
            }

            installSpan.addEvent(
                "${module.name}_initialized",
                module.initialization!!.run {
                    endElapsed!! - startElapsed
                },
                TimeUnit.MILLISECONDS
            )
        }

        installSpan.end(installEndTimestamp, TimeUnit.MILLISECONDS)
    }
}
