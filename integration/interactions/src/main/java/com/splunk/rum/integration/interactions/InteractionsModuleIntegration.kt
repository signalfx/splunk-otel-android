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

package com.splunk.rum.integration.interactions

import android.app.Application
import android.content.Context
import com.cisco.android.common.logger.Logger
import com.cisco.android.instrumentation.recording.capturer.FrameCapturer
import com.cisco.android.instrumentation.recording.interactions.Interactions
import com.cisco.android.instrumentation.recording.interactions.OnInteractionListener
import com.cisco.android.instrumentation.recording.interactions.compose.PointerInputObserverInjectorModifier
import com.cisco.android.instrumentation.recording.interactions.model.Interaction
import com.cisco.android.instrumentation.recording.interactions.model.LegacyData
import com.cisco.android.instrumentation.recording.wireframe.canvas.compose.SessionReplayDrawModifier
import com.cisco.android.instrumentation.recording.wireframe.model.Wireframe
import com.cisco.android.instrumentation.recording.wireframe.stats.WireframeStats
import com.splunk.rum.common.otel.SplunkOpenTelemetrySdk
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification.OrderPriority
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.agent.internal.utils.runIfComposeUiExists
import io.opentelemetry.api.common.AttributeKey
import java.util.concurrent.TimeUnit

internal object InteractionsModuleIntegration : ModuleIntegration<InteractionsModuleConfiguration>(
    defaultModuleConfiguration = InteractionsModuleConfiguration()
) {

    private const val TAG = "InteractionsIntegration"

    private val attributeKeyComponent = AttributeKey.stringKey("component")
    private val attributeKeyActionName = AttributeKey.stringKey("action.name")
    private val attributeKeyTargetType = AttributeKey.stringKey("target.type")

    override fun onAttach(context: Context) {
        val application = context.applicationContext as Application

        Interactions.attach(application)
        FrameCapturer.attach(application)

        FrameCapturer.listeners += object : FrameCapturer.Listener {
            override fun onNewWireframe(frame: Wireframe.Frame, stats: WireframeStats) {
                Interactions.updateWireframe(frame)
            }
        }

        Interactions.listeners += interactionsListener

        setupComposeIdentification()
    }

    private fun setupComposeIdentification() {
        runIfComposeUiExists {
            ComposeElementIdentification.insertModifierIfNeeded(
                SessionReplayDrawModifier::class,
                OrderPriority.HIGH
            ) { id, isSensitive, _ ->
                SessionReplayDrawModifier(id, isSensitive)
            }

            ComposeElementIdentification.insertModifierIfNeeded(
                PointerInputObserverInjectorModifier::class,
                OrderPriority.LOW
            ) { id, _, positionInList ->
                id?.let { PointerInputObserverInjectorModifier(id, positionInList) }
            }
        }
    }

    private val interactionsListener = object : OnInteractionListener {
        override fun onInteraction(interaction: Interaction, legacyData: LegacyData?) {
            if (!moduleConfiguration.isEnabled) {
                return
            }

            if (interaction is Interaction.Touch.Pointer ||
                interaction is Interaction.Touch.Continuous &&
                !interaction.isLast
            ) {
                return
            }

            val logger = SplunkOpenTelemetrySdk.instance?.sdkLoggerProvider ?: return

            val actionName = when (interaction) {
                is Interaction.Focus ->
                    "focus"
                is Interaction.Keyboard ->
                    "soft_keyboard"
                is Interaction.Orientation ->
                    return
                is Interaction.PhoneButton ->
                    "phone_button"
                is Interaction.Touch.Gesture.DoubleTap ->
                    "double_tap"
                is Interaction.Touch.Gesture.LongPress ->
                    "long_press"
                is Interaction.Touch.Gesture.Pinch ->
                    "pinch"
                is Interaction.Touch.Gesture.RageTap ->
                    "rage_tap"
                is Interaction.Touch.Gesture.Rotation ->
                    "rotation"
                is Interaction.Touch.Gesture.Swipe ->
                    return
                is Interaction.Touch.Gesture.Tap ->
                    "tap"
                is Interaction.Touch.Pointer ->
                    return
            }

            val targetType = if (interaction is Interaction.Targetable) {
                interaction.targetElementPath?.lastOrNull()?.view?.id
            } else {
                null
            }

            Logger.d(TAG, "onInteraction(actionName: $actionName, targetType: $targetType, interaction: $interaction)")

            logger.get(RumConstants.RUM_TRACER_NAME)
                .logRecordBuilder()
                .setTimestamp(interaction.timestamp, TimeUnit.MILLISECONDS)
                .setAttribute(RumConstants.LOG_EVENT_NAME_KEY, "action")
                .setAttribute(attributeKeyComponent, "ui")
                .setAttribute(attributeKeyActionName, actionName)
                .setAttribute(attributeKeyTargetType, targetType.orEmpty())
                .emit()
        }
    }
}
