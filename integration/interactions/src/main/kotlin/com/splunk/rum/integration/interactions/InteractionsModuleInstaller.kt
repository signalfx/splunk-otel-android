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

import android.content.Context
import com.splunk.android.instrumentation.recording.interactions.Interactions
import com.splunk.android.instrumentation.recording.interactions.model.Interaction
import com.splunk.rum.integration.agent.internal.module.ModuleInstaller
import com.splunk.rum.integration.interactions.api.InteractionCapture
import kotlin.reflect.KClass

internal class InteractionsModuleInstaller : ModuleInstaller() {

    override fun onInstall(context: Context) {
        InteractionsModuleIntegration.attach(context)
        InteractionCapture.createInstance(listener)
    }

    private val listener = object : InteractionCapture.OnChangedListener {
        override fun onChanged(instance: InteractionCapture) {
            val interactions = mutableSetOf<KClass<out Interaction>>()

            if (instance.isKeyboardEnabled) {
                interactions += Interaction.Keyboard::class
            }

            if (instance.isTouchEnabled) {
                interactions += Interaction.Touch.Pointer::class
            }

            if (instance.isGestureEnabled) {
                interactions += Interaction.Touch.Gesture.Tap::class
                interactions += Interaction.Touch.Gesture.DoubleTap::class
                interactions += Interaction.Touch.Gesture.LongPress::class
                interactions += Interaction.Touch.Gesture.Pinch::class
                interactions += Interaction.Touch.Gesture.Rotation::class
                interactions += Interaction.Touch.Gesture.Swipe::class
            }

            if (instance.isFocusEnabled) {
                interactions += Interaction.Focus::class
            }

            if (instance.isRageTapEnabled) {
                interactions += Interaction.Touch.Gesture.RageTap::class
            }

            if (instance.isDeviceButtonEnabled) {
                interactions += Interaction.PhoneButton::class
            }

            Interactions.allowedInteractions = interactions
        }
    }
}
