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

package com.splunk.rum.integration.sessionreplay.api

import android.graphics.Rect

/**
 * Masks rectangle on the screen by defined behaviour.
 *
 * @see SessionReplay.recordingMask
 */
data class RecordingMask(val elements: List<Element>) {

    /**
     * Mask element.
     *
     * @param rect Screen space rectangle
     * @param type Mask type. Default is [Type.COVERING].
     */
    data class Element @JvmOverloads constructor(val rect: Rect, val type: Type = Type.COVERING) {

        /**
         * Element mask type.
         */
        enum class Type {

            /**
             * Cover rectangle on the screen.
             */
            COVERING,

            /**
             * Uncover rectangle on the screen. For example, when one [Element] covers the whole screen and another uncover center of the screen, the result will be uncovered "hole".
             */
            ERASING
        }
    }
}
