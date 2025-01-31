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
import com.splunk.sdk.common.otel.OpenTelemetryInitializer
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.attributes.GenericAttributesLogProcessor
import com.splunk.rum.integration.agent.api.configuration.ConfigurationManager
import com.splunk.rum.integration.agent.api.extension.toResource
import com.splunk.rum.integration.agent.api.sessionId.SessionIdLogProcessor
import com.splunk.rum.integration.agent.api.sessionId.SessionIdSpanProcessor
import com.splunk.rum.integration.agent.api.sessionId.SessionStartEventManager
import com.splunk.rum.integration.agent.api.sessionPulse.SessionPulseEventManager
import com.splunk.rum.integration.agent.api.state.StateLogRecordProcessor
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.BuildConfig
import com.splunk.rum.integration.agent.internal.state.StateManager
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.sdk.common.storage.AgentStorage
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.resources.ResourceBuilder
import java.io.File

internal object MRUMAgentCore {

    private const val TAG = "MRUMAgentCore"
    private const val SERVICE_HASH_RESOURCE_KEY = "service.hash"

    fun install(application: Application, agentConfiguration: AgentConfiguration, moduleConfigurations: List<ModuleConfiguration>) {
        Logger.d(TAG, "install(agentConfiguration: $agentConfiguration, moduleConfigurations: $moduleConfigurations)")

        val storage = AgentStorage.attach(application)

        val finalConfiguration = ConfigurationManager
            .obtainInstance(storage)
            .preProcessConfiguration(application, agentConfiguration)

        val agentIntegration = AgentIntegration
            .obtainInstance(application)
            .setup(
                appName = requireNotNull(finalConfiguration.appName),
                agentVersion = requireNotNull(BuildConfig.VERSION_NAME),
                moduleConfigurations = moduleConfigurations
            )

        val stateManager = StateManager.obtainInstance(application)
        SessionStartEventManager.obtainInstance(agentIntegration.sessionManager)
        SessionPulseEventManager.obtainInstance(agentIntegration.sessionManager)

        OpenTelemetryInitializer(application)
            .joinResources(finalConfiguration.toResource())
            .addSpanProcessor(SessionIdSpanProcessor(agentIntegration.sessionManager))
            .addLogRecordProcessor(GenericAttributesLogProcessor())
            .addLogRecordProcessor(StateLogRecordProcessor(stateManager))
            .addLogRecordProcessor(SessionIdLogProcessor(agentIntegration.sessionManager))
            .build()

        agentIntegration.install(application)
    }


}
