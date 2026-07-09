/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.interactions.api

import com.splunk.android.instrumentation.recording.interactions.Interactions
import com.splunk.android.instrumentation.recording.interactions.model.Interaction
import kotlin.reflect.KClass

/**
 * Configuration for capturing user interactions.
 * Use this class to enable or disable specific types of interaction events.
 */
class InteractionCapture internal constructor() {

    /**
     * Captures keyboard activity that may reveal text-entry behavior.
     */
    var isKeyboardEnabled: Boolean
        get() = Interactions.allowedInteractions.containsAll(KEYBOARD)
        set(value) = setEnabled(KEYBOARD, value)

    /**
     * Captures pointer or touch interactions that are not recognized as higher-level gestures.
     */
    var isTouchEnabled: Boolean
        get() = Interactions.allowedInteractions.containsAll(TOUCH)
        set(value) = setEnabled(TOUCH, value)

    /**
     * Captures recognized gestures such as tap, double tap, long press, pinch, and zoom.
     */
    var isGestureEnabled: Boolean
        get() = Interactions.allowedInteractions.containsAll(GESTURE)
        set(value) = setEnabled(GESTURE, value)

    /**
     * Captures focus-change interactions.
     */
    var isFocusEnabled: Boolean
        get() = Interactions.allowedInteractions.containsAll(FOCUS)
        set(value) = setEnabled(FOCUS, value)

    /**
     * Captures rage tap events, which are currently the supported mobile frustration signal.
     */
    var isRageTapEnabled: Boolean
        get() = Interactions.allowedInteractions.containsAll(RAGE_TAP)
        set(value) = setEnabled(RAGE_TAP, value)

    /**
     * Captures volume up, volume down and back button.
     */
    var isDeviceButtonEnabled: Boolean
        get() = Interactions.allowedInteractions.containsAll(DEVICE_BUTTON)
        set(value) = setEnabled(DEVICE_BUTTON, value)

    /**
     * Enables all interaction-capture categories. This matches the default behavior.
     */
    fun enableAll() {
        setEnabled(ALL, true)
    }

    /**
     * Disables all interaction-capture categories.
     */
    fun disableAll() {
        setEnabled(ALL, false)
    }

    private fun setEnabled(set: Set<KClass<out Interaction>>, enabled: Boolean) {
        when (enabled) {
            true ->
                Interactions.allowedInteractions += set
            false ->
                Interactions.allowedInteractions -= set
        }
    }

    companion object {

        private val KEYBOARD = setOf(Interaction.Keyboard::class)
        private val GESTURE = setOf(
            Interaction.Touch.Gesture.Tap::class,
            Interaction.Touch.Gesture.DoubleTap::class,
            Interaction.Touch.Gesture.LongPress::class,
            Interaction.Touch.Gesture.Pinch::class,
            Interaction.Touch.Gesture.Rotation::class,
            Interaction.Touch.Gesture.Swipe::class
        )
        private val TOUCH = setOf(Interaction.Touch.Pointer::class)
        private val FOCUS = setOf(Interaction.Focus::class)
        private val RAGE_TAP = setOf(Interaction.Touch.Gesture.RageTap::class)
        private val DEVICE_BUTTON = setOf(Interaction.PhoneButton::class)

        private val ALL = KEYBOARD + GESTURE + TOUCH + FOCUS + RAGE_TAP + DEVICE_BUTTON

        private var instanceInternal: InteractionCapture? = null

        /**
         * Returns instance of the InteractionCapture.
         */
        @JvmStatic
        val instance: InteractionCapture
            get() = instanceInternal ?: throw IllegalStateException("Call install() first")

        internal fun createInstance() {
            instanceInternal = InteractionCapture()
        }
    }
}
