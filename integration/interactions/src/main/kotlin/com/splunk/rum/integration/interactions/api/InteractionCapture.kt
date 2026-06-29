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

/**
 * Configuration for capturing user interactions.
 * Use this class to enable or disable specific types of interaction events.
 */
class InteractionCapture internal constructor(onChangeListener: OnChangedListener) {

    private var onChangeListener: OnChangedListener? = onChangeListener

    /**
     * Captures keyboard activity that may reveal text-entry behavior.
     */
    var isKeyboardEnabled = true
        set(value) {
            field = value
            onChangeListener?.onChanged(this)
        }

    /**
     * Captures pointer or touch interactions that are not recognized as higher-level gestures.
     */
    var isTouchEnabled = true
        set(value) {
            field = value
            onChangeListener?.onChanged(this)
        }

    /**
     * Captures recognized gestures such as tap, double tap, long press, pinch, and zoom.
     */
    var isGestureEnabled = true
        set(value) {
            field = value
            onChangeListener?.onChanged(this)
        }

    /**
     * Captures focus-change interactions.
     */
    var isFocusEnabled = true
        set(value) {
            field = value
            onChangeListener?.onChanged(this)
        }

    /**
     * Captures rage tap events, which are currently the supported mobile frustration signal.
     */
    var isRageTapEnabled = true
        set(value) {
            field = value
            onChangeListener?.onChanged(this)
        }

    /**
     * Captures volume up, volume down and back button.
     */
    var isDeviceButtonEnabled = true
        set(value) {
            field = value
            onChangeListener?.onChanged(this)
        }

    /**
     * Enables all interaction-capture categories. This matches the default behavior.
     */
    fun enableAll() {
        changeAll(true)
    }

    /**
     * Disables all interaction-capture categories.
     */
    fun disableAll() {
        changeAll(false)
    }

    private fun changeAll(enable: Boolean) {
        val onChangeListenerBackup = onChangeListener
        onChangeListener = null

        isKeyboardEnabled = enable
        isTouchEnabled = enable
        isGestureEnabled = enable
        isFocusEnabled = enable
        isRageTapEnabled = enable
        isDeviceButtonEnabled = enable

        onChangeListener = onChangeListenerBackup
        onChangeListener?.onChanged(this)
    }

    internal interface OnChangedListener {
        fun onChanged(instance: InteractionCapture)
    }

    companion object {

        private var instanceInternal: InteractionCapture? = null

        /**
         * Returns instance of the InteractionCapture.
         */
        @JvmStatic
        val instance: InteractionCapture
            get() = instanceInternal ?: throw IllegalStateException("Call install() first")

        internal fun createInstance(onChangeListener: OnChangedListener) {
            instanceInternal = InteractionCapture(onChangeListener)
        }
    }
}
