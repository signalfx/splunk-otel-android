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

package com.splunk.rum.integration.agent.api.internal

import android.app.Application
import com.cisco.android.common.logger.Logger
import com.cisco.android.common.logger.consumers.AndroidLogConsumer
import com.splunk.rum.common.otel.OpenTelemetryInitializer
import com.splunk.rum.common.storage.AgentStorage
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.configuration.ConfigurationManager
import com.splunk.rum.integration.agent.api.exporter.LoggerSpanExporter
import com.splunk.rum.integration.agent.api.resource.AgentResource
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.attributes.ScreenNameTracker
import com.splunk.rum.integration.agent.internal.processor.AppStartSpanProcessor
import com.splunk.rum.integration.agent.internal.processor.ErrorIdentifierAttributesSpanProcessor
import com.splunk.rum.integration.agent.internal.processor.GlobalAttributeSpanProcessor
import com.splunk.rum.integration.agent.internal.processor.LastScreenNameSpanProcessor
import com.splunk.rum.integration.agent.internal.processor.SessionIdSpanProcessor
import com.splunk.rum.integration.agent.internal.processor.SessionReplaySessionIdLogProcessor
import com.splunk.rum.integration.agent.internal.processor.SplunkInternalGlobalAttributeSpanProcessor
import com.splunk.rum.integration.agent.internal.processor.UserIdSpanProcessor
import com.splunk.rum.integration.agent.internal.session.ISplunkSessionManager
import com.splunk.rum.integration.agent.internal.user.IUserManager
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

internal object SplunkRumAgentCore {

    private const val TAG = "SplunkRumAgentCore"
    var isRunning: Boolean = false

    fun install(
        application: Application,
        agentConfiguration: AgentConfiguration,
        userManager: IUserManager,
        sessionManager: ISplunkSessionManager,
        moduleConfigurations: List<ModuleConfiguration>
    ): OpenTelemetry {
        // Sampling.
        val shouldBeRunning = when (val samplingRate = agentConfiguration.session.samplingRate.coerceIn(0.0, 1.0)) {
            0.0 -> false
            1.0 -> true
            else -> Math.random() < samplingRate
        }

        if (!shouldBeRunning) return OpenTelemetry.noop()

        if (agentConfiguration.enableDebugLogging) {
            Logger.consumers += AndroidLogConsumer()
        }

        Logger.d(TAG, "install(agentConfiguration: $agentConfiguration, moduleConfigurations: $moduleConfigurations)")

        sessionManager.reset()

        val storage = AgentStorage.attach(application)

        val finalConfiguration = ConfigurationManager
            .obtainInstance(storage)
            .preProcessConfiguration(application, agentConfiguration)

        val agentIntegration = AgentIntegration
            .obtainInstance(application)

        val initializer = OpenTelemetryInitializer(
            application,
            agentConfiguration.deferredUntilForeground,
            agentConfiguration.spanInterceptor
        )
            // The GlobalAttributeSpanProcessor must be registered first to ensure that global attributes
            // do not override internal agent attributes required by the backend.
            .addSpanProcessor(GlobalAttributeSpanProcessor(agentConfiguration.globalAttributes))
            .addSpanProcessor(LastScreenNameSpanProcessor(ScreenNameTracker))
            .joinResources(AgentResource.allResource(application, finalConfiguration))
            .addSpanProcessor(UserIdSpanProcessor(userManager))
            .addSpanProcessor(ErrorIdentifierAttributesSpanProcessor(application))
            .addSpanProcessor(SessionIdSpanProcessor(agentIntegration.sessionManager))
            .addSpanProcessor(SplunkInternalGlobalAttributeSpanProcessor())
            .addSpanProcessor(AppStartSpanProcessor())
            // Session Replay module is special case of Log Records that are NOT converted to Spans.
            .addLogRecordProcessor(SessionReplaySessionIdLogProcessor(agentIntegration.sessionManager))

        if (agentConfiguration.enableDebugLogging) {
            initializer.addSpanProcessor(SimpleSpanProcessor.builder(LoggerSpanExporter()).build())
        }

        val openTelemetry = initializer.build()

        isRunning = true

        agentIntegration.install(application, openTelemetry, moduleConfigurations)

        return openTelemetry
    }
}
