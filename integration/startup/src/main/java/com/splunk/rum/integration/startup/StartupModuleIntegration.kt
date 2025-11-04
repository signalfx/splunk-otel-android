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

import android.content.Context
import android.os.SystemClock
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.toInstant
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.common.module.toSplunkString
import com.splunk.rum.integration.agent.internal.AgentIntegration.Companion.modules
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.startup.model.StartupData
import com.splunk.rum.startup.ApplicationStartupTimekeeper
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.trace.SdkTracerProvider
import java.time.Instant
import java.util.concurrent.TimeUnit

internal object StartupModuleIntegration : ModuleIntegration<StartupModuleConfiguration>(
    defaultModuleConfiguration = StartupModuleConfiguration()
) {

    private const val TAG = "StartupIntegration"

    private val cache: MutableList<StartupData> = mutableListOf()
    private var isInitializationReported = false

    override fun onAttach(context: Context) {
        ApplicationStartupTimekeeper.listeners += applicationStartupTimekeeperListener
    }

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        cache.forEachFast { reportEvent(it.startTimestamp, it.endTimestamp, it.name) }
        cache.clear()
    }

    private val applicationStartupTimekeeperListener = object : ApplicationStartupTimekeeper.Listener {
        override fun onColdStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(
                TAG,
                "onColdStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)"
            )
            reportEvent(startTimestamp, endTimestamp, "cold")
        }

        override fun onWarmStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(
                TAG,
                "onWarmStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)"
            )
            reportEvent(startTimestamp, endTimestamp, "warm")
        }

        override fun onHotStarted(startTimestamp: Long, endTimestamp: Long, duration: Long) {
            Logger.d(
                TAG,
                "onHotStarted(startTimestamp: $startTimestamp, endTimestamp: $endTimestamp, duration: $duration ms)"
            )
            reportEvent(startTimestamp, endTimestamp, "hot")
        }
    }

    private fun reportEvent(startTimestamp: Long, endTimestamp: Long, name: String) {
        if (isInitializationReported) return

        isInitializationReported = true

        val provider = SplunkOpenTelemetrySdk.instance?.sdkTracerProvider ?: run {
            cache += StartupData(startTimestamp, endTimestamp, name)
            return
        }

        val span = provider.get(RumConstants.RUM_TRACER_NAME)
            .spanBuilder(RumConstants.APP_START_NAME)
            .setStartTimestamp(startTimestamp, TimeUnit.MILLISECONDS)
            .startSpan()

        reportAppStart(span, provider)

        // Actual screen.name as set by SplunkInternalGlobalAttributeSpanProcessor is overwritten here to set it to
        // "unknown" to ensure App Start event doesn't show up under a screen on UI
        span
            .setAttribute(RumConstants.COMPONENT_KEY, "appstart")
            .setAttribute(RumConstants.SCREEN_NAME_KEY, RumConstants.DEFAULT_SCREEN_NAME)
            .setAttribute("start.type", name)
            .end(endTimestamp.toInstant())
    }

    private fun reportAppStart(span: Span, provider: SdkTracerProvider) {
        val modules = modules.values

        val firstInitialization =
            modules.minByOrNull { it.initialization?.startTimestamp ?: Long.MAX_VALUE }?.initialization
                ?: throw IllegalStateException("Module initialization did not started")
        val startTimestamp =
            firstInitialization.startTimestamp + SystemClock.elapsedRealtime() - firstInitialization.startElapsed

        val span = provider.get(RumConstants.RUM_TRACER_NAME)
            .spanBuilder("SplunkRum.initialize")
            .setParent(io.opentelemetry.context.Context.current().with(span))
            .setStartTimestamp(startTimestamp.toInstant())
            .startSpan()

        // Actual screen.name as set by SplunkInternalGlobalAttributeSpanProcessor is overwritten here to set it to
        // "unknown" to ensure App Start event doesn't show up under a screen on UI
        span.setAttribute(RumConstants.COMPONENT_KEY, "appstart")
            .setAttribute(RumConstants.SCREEN_NAME_KEY, RumConstants.DEFAULT_SCREEN_NAME)

        val resources = modules.joinToString(",", "[", "]") {
            it.configuration?.toSplunkString()
                ?: "${it.name}.enabled:true"
        }

        span.setAttribute("config_settings", resources)

        for (module in modules) {
            if (module.initialization == null) {
                throw IllegalStateException("Module '${module.name}' initialization has not been started")
            }

            if (module.initialization!!.endElapsed == null) {
                throw IllegalStateException("Module '${module.name}' is not initialized")
            }

            span.addEvent(
                "${module.name}_initialized",
                module.initialization!!.run {
                    endElapsed!! - startElapsed
                },
                TimeUnit.MILLISECONDS
            )
        }

        @Suppress("NewApi") // Requires API 26 or core library desugaring
        span.end(Instant.now())
    }
}
