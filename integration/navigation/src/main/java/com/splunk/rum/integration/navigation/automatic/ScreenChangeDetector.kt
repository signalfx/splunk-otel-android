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
 * Emits only on resumed (onFragmentResumed / onActivityResumed) as the trigger.
 */
internal class ScreenChangeDetector(private val eventEmitter: NavigationEventEmitter) {
    private val handler = Handler(Looper.getMainLooper())
    private var lastResumedActivityName: String? = null
    private var previouslyLastResumedActivityName: String? = null
    private var lastResumedFragmentName: String? = null
    private var previouslyLastResumedFragmentName: String? = null

    /**
     * Current visible screen name: fragment if any, else activity.
     */
    private fun currentVisibleScreenName(): String? {
        val fragment = lastResumedFragmentName
        if (fragment != null) return fragment
        return lastResumedActivityName
    }

    fun onActivityResumed(activity: Activity) {
        val name = ScreenNameDescriptor.getName(activity)
        lastResumedActivityName = name
        // Defer so fragment callbacks, which run synchronously during the same resume, execute
        // first thus avoiding emitting an intermediate activity only event when a fragment is present
        handler.post { tryEmitIfChanged() }
    }

    fun onActivityPaused(activity: Activity) {
        val name = ScreenNameDescriptor.getName(activity)
        previouslyLastResumedActivityName = name
        if (lastResumedActivityName == name) {
            lastResumedActivityName = null
        }
    }

    fun onFragmentResumed(fragment: Fragment) {
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        if (fragment is DialogFragment) {
            previouslyLastResumedFragmentName = lastResumedFragmentName
        }

        val name = ScreenNameDescriptor.getName(fragment)
        lastResumedFragmentName = name
        tryEmitIfChanged()
    }

    fun onFragmentPaused(fragment: Fragment) {
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        if (fragment is DialogFragment) {
            lastResumedFragmentName = previouslyLastResumedFragmentName
        } else if (lastResumedFragmentName == ScreenNameDescriptor.getName(fragment)) {
            lastResumedFragmentName = null
        }

        previouslyLastResumedFragmentName = ScreenNameDescriptor.getName(fragment)
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
        val current = currentVisibleScreenName() ?: return
        if (current == lastEmittedScreenName) return

        val previous = lastEmittedScreenName
        lastEmittedScreenName = current
        eventEmitter.emitNavigationEvent(current, previous)
    }
}
