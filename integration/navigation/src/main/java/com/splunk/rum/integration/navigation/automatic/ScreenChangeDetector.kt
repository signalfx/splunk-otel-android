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

package com.splunk.rum.integration.navigation.automatic

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import com.splunk.rum.integration.navigation.descriptor.ScreenNameDescriptor
import io.opentelemetry.api.common.Attributes

/**
 * Detects visible screen changes (Activity/Fragment/Compose route) and notifies
 * [NavigationEventEmitter]. Priority: Compose route > Fragment > Activity.
 *
 * Elements marked as ignored by [ScreenNameDescriptor] (e.g. DialogFragment, NavHostFragment,
 * or annotated with isIgnored = true) are skipped entirely.
 *
 * Emits on resumed (onFragmentResumed / onActivityResumed) as the primary trigger.
 * Also emits via deferred post on fragment pause to handle the case where a fragment
 * is removed without a replacement (falling back to parent fragment or activity).
 */

internal class ScreenChangeDetector(private val eventEmitter: NavigationEventEmitter) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastResumedActivityName: String? = null
    private var lastResumedFragmentName: String? = null
    private var lastComposeRouteName: String? = null
    private var composeRouteActivityName: String? = null
    private var pendingPauseEmit: Runnable? = null

    /**
     * Current visible screen name: Compose route > Fragment > Activity.
     */
    private fun getCurrentVisibleScreenName(): String? =
        lastComposeRouteName ?: lastResumedFragmentName ?: lastResumedActivityName

    fun onActivityResumed(activity: Activity) {
        if (ScreenNameDescriptor.isIgnored(activity)) return

        val name = ScreenNameDescriptor.getName(activity)
        lastResumedActivityName = name

        if (lastComposeRouteName != null && composeRouteActivityName == null) {
            composeRouteActivityName = name
        } else if (composeRouteActivityName != null && composeRouteActivityName != name) {
            lastComposeRouteName = null
            composeRouteActivityName = null
            lastEmittedComposeAttributes = null
        }

        // Defer so fragment callbacks, which run synchronously during the same resume, execute
        // first thus avoiding emitting an intermediate activity only event when a fragment is present
        handler.post { tryEmitIfChanged() }
    }

    fun onActivityPaused(activity: Activity) {
        if (ScreenNameDescriptor.isIgnored(activity)) return

        val name = ScreenNameDescriptor.getName(activity)
        if (lastResumedActivityName == name) {
            lastResumedActivityName = null
        }
    }

    fun onFragmentResumed(fragment: Fragment) {
        cancelPendingPauseEmit()
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        val name = ScreenNameDescriptor.getName(fragment)
        lastResumedFragmentName = name
        tryEmitIfChanged()
    }

    fun onFragmentPaused(fragment: Fragment) {
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        val name = ScreenNameDescriptor.getName(fragment)
        if (lastResumedFragmentName == name) {
            lastResumedFragmentName = findResumedAncestorName(fragment)
        }
        val runnable = Runnable { tryEmitIfChanged() }
        pendingPauseEmit = runnable
        handler.post(runnable)
    }

    private fun cancelPendingPauseEmit() {
        pendingPauseEmit?.let { handler.removeCallbacks(it) }
        pendingPauseEmit = null
    }

    private fun findResumedAncestorName(fragment: Fragment): String? {
        var parent = fragment.parentFragment
        while (parent != null) {
            if (parent.isResumed && !ScreenNameDescriptor.isIgnored(parent)) {
                return ScreenNameDescriptor.getName(parent)
            }
            parent = parent.parentFragment
        }
        return null
    }

    private var lastEmittedScreenName: String? = null
    private var lastEmittedComposeAttributes: Attributes? = null

    /**
     * Records that a navigation event was emitted for [screenName] (e.g. from manual tracking).
     * Keeps automatic and manual tracking in sync so we don't double-emit or emit "null -> X"
     * when manual already ran first.
     */
    fun recordEmittedScreen(screenName: String) {
        lastEmittedScreenName = screenName
        lastEmittedComposeAttributes = Attributes.empty()
    }

    /**
     * Called when a Compose NavController destination changes. Sets the compose route as the
     * highest-priority screen name and emits the event with optional [attributes].
     * Associates the route with the currently resumed activity so it can be cleared
     * when a different activity resumes.
     *
     * Deduplicates by both screen name and attributes so navigating the same route template
     * with different arguments (e.g. profile/user_123 → profile/user_456) still emits.
     */
    fun onComposeRouteChanged(screenName: String, attributes: Attributes = Attributes.empty()) {
        lastComposeRouteName = screenName
        composeRouteActivityName = lastResumedActivityName
        if (screenName == lastEmittedScreenName && attributes == lastEmittedComposeAttributes) return
        lastEmittedScreenName = screenName
        lastEmittedComposeAttributes = attributes
        eventEmitter.emitNavigationEvent(screenName, attributes)
    }

    /**
     * Clears the active Compose route, allowing Fragment/Activity names to take precedence again.
     * Called when a NavController is unregistered.
     */
    fun clearComposeRoute() {
        lastComposeRouteName = null
        composeRouteActivityName = null
        lastEmittedComposeAttributes = null
    }

    /**
     * If the visible screen changed, emit a navigation event and advance [lastEmittedScreenName].
     * Only called from resumed callbacks.
     */
    private fun tryEmitIfChanged() {
        val current = getCurrentVisibleScreenName() ?: return
        if (current == lastEmittedScreenName) return

        lastEmittedScreenName = current
        eventEmitter.emitNavigationEvent(current)
    }
}
