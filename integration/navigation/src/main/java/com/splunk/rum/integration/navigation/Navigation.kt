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

package com.splunk.rum.integration.navigation

import io.opentelemetry.api.common.Attributes

class Navigation internal constructor() {

    internal var listener: Listener? = null

    /**
     * Record a navigation to [screenName] (manual tracking).
     */
    fun track(screenName: String) {
        listener?.onScreenNameChanged(screenName, Attributes.empty())
    }

    /**
     * Record a navigation to [screenName] with optional [attributes] (manual tracking).
     */
    fun track(screenName: String, attributes: Attributes) {
        listener?.onScreenNameChanged(screenName, attributes)
    }

    internal interface Listener {
        fun onScreenNameChanged(screenName: String, attributes: Attributes)
    }

    companion object {

        @JvmStatic
        val instance by lazy { Navigation() }
    }
}
