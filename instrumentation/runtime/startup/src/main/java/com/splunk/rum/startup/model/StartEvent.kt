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

package com.splunk.rum.startup.model

import com.splunk.rum.utils.extensions.formatDateTime

data class StartEvent internal constructor(val type: Type, val startTimestamp: Long, val endTimestamp: Long) {

    enum class Type {
        /**
         * The application is launched from a completely inactive state.
         * Kill the app > press the application icon.
         */
        COLD,

        /**
         * The application is launched after being recently closed or moved to the background, but still resides in memory.
         * Open the app > press back button > press the app icon.
         */
        WARM,

        /**
         * The application is already running in the background and is brought to the foreground.
         * Open the app > press home button > press the app icon.
         */
        HOT
    }

    val duration: Long
        get() = endTimestamp - startTimestamp

    override fun toString(): String =
        "StartEvent(type: $type, startTime: ${startTimestamp.formatDateTime()}, endTime: ${endTimestamp.formatDateTime()}, duration: $duration ms)"
}
