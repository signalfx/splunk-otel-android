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
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.config.ModuleConfigurationManager
import com.splunk.rum.integration.agent.internal.extension.find
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification.OrderPriority
import com.splunk.rum.integration.agent.internal.utils.runIfComposeUiExists
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import com.splunk.sdk.common.otel.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import java.util.concurrent.TimeUnit

internal object InteractionsConfigurator {

    private const val TAG = "InteractionsConfigurator"
    private const val MODULE_NAME = "interactions"

    private val attributeKeyComponent = AttributeKey.stringKey("component")
    private val attributeKeyActionName = AttributeKey.stringKey("action.name")
    private val attributeKeyTargetType = AttributeKey.stringKey("target.type")

    private val defaultModuleConfiguration = InteractionsModuleConfiguration(
        isEnabled = true
    )

    private var moduleConfiguration = defaultModuleConfiguration

    init {
        AgentIntegration.registerModule(MODULE_NAME)
    }

    fun attach(context: Context) {
        AgentIntegration.obtainInstance(context).listeners += installationListener

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
            ComposeElementIdentification.insertModifierIfNeeded(SessionReplayDrawModifier::class, OrderPriority.HIGH) { id, isSensitive, _ ->
                SessionReplayDrawModifier(id, isSensitive)
            }

            ComposeElementIdentification.insertModifierIfNeeded(PointerInputObserverInjectorModifier::class, OrderPriority.LOW) { id, _, positionInList ->
                id?.let { PointerInputObserverInjectorModifier(id, positionInList) }
            }
        }
    }

    private val interactionsListener = object : OnInteractionListener {
        override fun onInteraction(interaction: Interaction, legacyData: LegacyData?) {
            Logger.d(TAG, "onInteraction(interaction: $interaction, legacyData: $legacyData)")
            reportEvent(interaction)
        }

        private fun reportEvent(interaction: Interaction) {
            if (!moduleConfiguration.isEnabled)
                return

            val logger = OpenTelemetry.instance?.sdkLoggerProvider ?: return

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

            val targetType = if (interaction is Interaction.Targetable)
                interaction.targetElementPath?.lastOrNull()?.view?.id
            else
                null

            logger.get("SplunkRum")
                .logRecordBuilder()
                .setTimestamp(interaction.timestamp, TimeUnit.MILLISECONDS)
                .setAttribute(attributeKeyComponent, "ui")
                .setAttribute(attributeKeyActionName, actionName)
                .setAttribute(attributeKeyTargetType, targetType)
                .emit()
        }
    }

    private val configManagerListener = object : ModuleConfigurationManager.Listener {
        override fun onSetup(configurations: List<ModuleConfiguration>) {
            moduleConfiguration = configurations.find<InteractionsModuleConfiguration>() ?: defaultModuleConfiguration
        }
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context) {
            Logger.d(TAG, "onInstall()")

            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += configManagerListener
        }
    }
}
