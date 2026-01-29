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
import android.webkit.WebView
import com.splunk.android.common.logger.Logger
import com.splunk.android.instrumentation.recording.core.api.DataListener
import com.splunk.android.instrumentation.recording.core.api.Metadata
import com.splunk.android.instrumentation.recording.core.api.SessionReplay
import com.splunk.android.instrumentation.recording.wireframe.canvas.compose.SessionReplayDrawModifier
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.extensions.toInstant
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification.OrderPriority
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.agent.internal.utils.runIfComposeUiExists
import com.splunk.rum.integration.sessionreplay.api.SessionReplay as SplunkSessionReplay
import com.splunk.rum.integration.sessionreplay.index.TimeIndex
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.Value
import java.util.concurrent.TimeUnit
import org.json.JSONObject

internal object SessionReplayModuleIntegration : ModuleIntegration<SessionReplayModuleConfiguration>(
    defaultModuleConfiguration = SessionReplayModuleConfiguration()
) {

    private const val TAG = "SessionReplayIntegration"

    private val isRecordingForSessions: MutableSet<String> = mutableSetOf()

    private var timeIndex: TimeIndex<Long> = TimeIndex()

    override fun onAttach(context: Context) {
        Logger.d(TAG, "onAttach()")

        setupComposeIdentification()
    }

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")
        timeIndex.put(1)

        SplunkSessionReplay.createInstance(moduleConfiguration)

        with(SessionReplay.instance) {
            dataListeners += sessionReplayDataListener

            // For Splunk agents, the WebView must not be sensitive by default.
            sensitivity.setViewClassSensitivity(WebView::class.java, null)
        }
    }

    override fun onSessionChange(sessionId: String) {
        super.onSessionChange(sessionId)
        Logger.d(TAG, "onSessionChange()")
        timeIndex.put(1)
        SessionReplay.instance.newDataChunk()
    }

    private fun setupComposeIdentification() {
        runIfComposeUiExists {
            ComposeElementIdentification.insertModifierIfNeeded(
                SessionReplayDrawModifier::class,
                OrderPriority.HIGH
            ) { id, isSensitive, _ ->
                SessionReplayDrawModifier(id, isSensitive)
            }
        }
    }

    private val sessionReplayDataListener = object : DataListener {
        override fun onData(data: ByteArray, metadata: Metadata): Boolean {
            Logger.d(TAG, "onData()")

            val instance = SplunkOpenTelemetrySdk.instance ?: return false

            val segmentMetadata = JSONObject().put("startUnixMs", metadata.startUnixMs)
                .put("endUnixMs", metadata.endUnixMs)
                .put("source", metadata.platform)
                .toString()

            val index = timeIndex.getAt(metadata.startUnixMs.toInstant()) ?: 1
            timeIndex.putAt((metadata.endUnixMs - 1).toInstant(), index + 1)

            val attributes = Attributes.of(
                RumConstants.LOG_EVENT_NAME_KEY, "session_replay_data",
                AttributeKey.doubleKey("rr-web.total-chunks"), 1.0,
                AttributeKey.doubleKey("rr-web.chunk"), 1.0,
                AttributeKey.longKey("rr-web.event"), index,
                AttributeKey.doubleKey("rr-web.offset"), index.toDouble(),
                AttributeKey.stringKey("segmentMetadata"), segmentMetadata
            )

            val sessionReplayDataBuilder = instance.sdkLoggerProvider
                .loggerBuilder(RumConstants.SESSION_REPLAY_INSTRUMENTATION_SCOPE_NAME)
                .build()
                .logRecordBuilder()

            sessionReplayDataBuilder.setBody(Value.of(data))
                .setTimestamp(metadata.startUnixMs, TimeUnit.MILLISECONDS)
                .setAllAttributes(attributes)
                .emit()

            val sessionId = sessionManager.sessionId(metadata.startUnixMs)

            if (!isRecordingForSessions.contains(sessionId)) {
                isRecordingForSessions.add(sessionId)
                instance.sdkLoggerProvider
                    .get(RumConstants.RUM_TRACER_NAME)
                    .logRecordBuilder()
                    .setTimestamp(metadata.startUnixMs, TimeUnit.MILLISECONDS)
                    .setAttribute(RumConstants.LOG_EVENT_NAME_KEY, "splunk.sessionReplay.isRecording")
                    .setAttribute(RumConstants.COMPONENT_KEY, "session.replay")
                    .setAttribute(RumConstants.SESSION_REPLAY_KEY, "splunk")
                    .setAttribute(RumConstants.SESSION_ID_KEY, sessionId)
                    .emit()
            }

            return true
        }
    }
}
