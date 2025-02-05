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
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification.OrderPriority
import com.splunk.rum.integration.agent.internal.utils.runIfComposeUiExists

internal object InteractionsConfigurator {

    private const val TAG = "InteractionsConfigurator"
    private const val MODULE_NAME = "interactions"

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

            // TODO process interaction
        }
    }

    private val configManagerListener = object : ModuleConfigurationManager.Listener {
    }

    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(context: Context) {
            Logger.d(TAG, "onInstall()")

            val integration = AgentIntegration.obtainInstance(context)
            integration.moduleConfigurationManager.listeners += configManagerListener
        }
    }
}
