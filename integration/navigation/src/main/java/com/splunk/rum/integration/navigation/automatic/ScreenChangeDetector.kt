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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.splunk.rum.integration.navigation.descriptor.ScreenNameDescriptor

/**
 * Detects visible screen changes (Activity/Fragment) and notifies [NavigationEventEmitter].
 * Fragment takes precedence over Activity when both are present.
 *
 * DialogFragments are ignored as they are overlays, not screen navigations.
 * Users can manually track full-screen dialogs via [Navigation.track].
 *
 * Emits on resumed (onFragmentResumed / onActivityResumed) as the primary trigger.
 * Also emits via deferred post on fragment pause to handle the case where a fragment
 * is removed without a replacement (falling back to parent fragment or activity).
 */

internal class ScreenChangeDetector(private val eventEmitter: NavigationEventEmitter) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastResumedActivityName: String? = null
    private var lastResumedFragmentName: String? = null

    /**
     * Current visible screen name: fragment if any, else activity.
     */
    private fun getCurrentVisibleScreenName(): String? {
        val fragment = lastResumedFragmentName
        if (fragment != null) return fragment
        return lastResumedActivityName
    }

    fun onActivityResumed(activity: Activity) {
        if (ScreenNameDescriptor.isIgnored(activity)) return

        val name = ScreenNameDescriptor.getName(activity)
        lastResumedActivityName = name
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
        if (fragment is DialogFragment) return
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        val name = ScreenNameDescriptor.getName(fragment)
        lastResumedFragmentName = name
        tryEmitIfChanged()
    }

    fun onFragmentPaused(fragment: Fragment) {
        if (fragment is DialogFragment) return
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        val name = ScreenNameDescriptor.getName(fragment)
        if (lastResumedFragmentName == name) {
            lastResumedFragmentName = findResumedAncestorName(fragment)
        }
        handler.post { tryEmitIfChanged() }
    }

    private fun findResumedAncestorName(fragment: Fragment): String? {
        var parent = fragment.parentFragment
        while (parent != null) {
            if (parent.isResumed && parent !is DialogFragment && !ScreenNameDescriptor.isIgnored(parent)) {
                return ScreenNameDescriptor.getName(parent)
            }
            parent = parent.parentFragment
        }
        return null
    }

    private var lastEmittedScreenName: String? = null

    /**
     * Records that a navigation event was emitted for [screenName] (e.g. from manual tracking).
     * Keeps automatic and manual tracking in sync so we don't double-emit or emit "null -> X"
     * when manual already ran first.
     */
    fun recordEmittedScreen(screenName: String) {
        lastEmittedScreenName = screenName
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
