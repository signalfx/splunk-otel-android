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

import androidx.navigation.NavController
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.navigation.automatic.ComposeNavigationTracker
import io.opentelemetry.api.common.Attributes

class Navigation internal constructor() {

    internal var listener: Listener? = null
    internal var composeTracker: ComposeNavigationTracker? = null

    /**
     * Record a navigation to [screenName] with optional [attributes] (manual tracking).
     */
    @JvmOverloads
    fun track(screenName: String, attributes: Attributes = Attributes.empty()) {
        listener?.onScreenNameChanged(screenName, attributes)
    }

    /**
     * Register a [NavController] for automatic Compose navigation tracking.
     *
     * The SDK attaches an [NavController.OnDestinationChangedListener] to track route changes.
     * Only one NavController can be tracked at a time; registering a new one replaces the previous.
     * Passing the same instance again is a no-op.
     *
     * Call this after [NavController] is created (e.g. in your Activity's `setContent` block).
     */
    fun registerNavController(navController: NavController) {
        val tracker = composeTracker
        if (tracker == null) {
            Logger.w(TAG, "Navigation module not installed. Cannot register NavController.")
            return
        }
        tracker.register(navController)
    }

    /**
     * Unregister a previously registered [NavController], removing the destination listener.
     */
    fun unregisterNavController(navController: NavController) {
        composeTracker?.unregister(navController)
    }

    internal interface Listener {
        fun onScreenNameChanged(screenName: String, attributes: Attributes)
    }

    companion object {
        private const val TAG = "Navigation"

        @JvmStatic
        val instance by lazy { Navigation() }
    }
}
