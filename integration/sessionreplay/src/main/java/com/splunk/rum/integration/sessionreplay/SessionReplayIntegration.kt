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

package com.splunk.rum.integration.sessionreplay

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.cisco.android.instrumentation.recording.core.api.DataListener
import com.cisco.android.instrumentation.recording.core.api.Metadata
import com.cisco.android.instrumentation.recording.core.api.SessionReplay
import com.cisco.android.instrumentation.recording.wireframe.canvas.compose.SessionReplayDrawModifier
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification.OrderPriority
import com.splunk.rum.integration.agent.internal.utils.runIfComposeUiExists
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.sdk.common.otel.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.extension.incubator.logs.AnyValue
import io.opentelemetry.extension.incubator.logs.ExtendedLogRecordBuilder
import java.util.concurrent.TimeUnit

internal object SessionReplayIntegration {

    private const val TAG = "SessionReplayIntegration"
    private const val MODULE_NAME = "sessionReplay"

    init {
        Logger.d(TAG, "init()")

        AgentIntegration.registerModuleInitializationStart(MODULE_NAME)
    }

    fun attach(context: Context) {
        Logger.d(TAG, "attach()")

        AgentIntegration.obtainInstance(context).listeners += installationListener

        setupComposeIdentification()
    }

    private fun setupComposeIdentification() {
        runIfComposeUiExists {
            ComposeElementIdentification.insertModifierIfNeeded(SessionReplayDrawModifier::class, OrderPriority.HIGH) { id, isSensitive, _ ->
                SessionReplayDrawModifier(id, isSensitive)
            }
        }
    }

    private val moduleConfigurationManagerListener = object : ModuleConfigurationManager.Listener {
        override fun onSetup(configurations: List<ModuleConfiguration>) {
        }
    }

    private val sessionReplayDataListener = object : DataListener {
        override fun onData(data: ByteArray, metadata: Metadata): Boolean {
            Logger.d(TAG, "onData()")

            val instance = OpenTelemetry.instance ?: return false

            val attributes = Attributes.of(
                AttributeKey.stringKey("event.name"), "session_replay_data",
//                AttributeKey.stringKey("replay.record_id"), recordData.id,
//                AttributeKey.longKey("replay.start_timestamp"), recordData.start * 1_000_000,
//                AttributeKey.longKey("replay.end_timestamp"), recordData.end * 1_000_000
            )

            val logRecordBuilder = instance.sdkLoggerProvider
                .loggerBuilder("SessionReplayDataScopeName")
                .build()
                .logRecordBuilder() as ExtendedLogRecordBuilder

            logRecordBuilder.setBody(AnyValue.of(data))
                .setTimestamp(metadata.startUnixMs, TimeUnit.MILLISECONDS)
                .setAllAttributes(attributes)
                .emit()

            return true
        }
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context) {
            Logger.d(TAG, "onInstall()")

            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += moduleConfigurationManagerListener
            SessionReplay.instance.dataListeners += sessionReplayDataListener

            AgentIntegration.registerModuleInitializationEnd(MODULE_NAME)
        }
    }
}
