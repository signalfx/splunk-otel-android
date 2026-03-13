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

package com.splunk.rum.integration.navigation.automatic

import android.app.Activity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.splunk.rum.integration.navigation.NavigationEmissionPolicy
import com.splunk.rum.integration.navigation.descriptor.ScreenNameDescriptor

/**
 * Detects visible screen changes (Activity/Fragment) and notifies [NavigationEventEmitter].
 * Fragment takes precedence over Activity when both are present.
 *
 * Only emits when the [NavigationEmissionPolicy] allows: [ON_SCREEN_ACTIVE] emits only on resumed
 * callbacks; [ALL_CHANGES] emits on any visible-screen change.
 */
internal class ScreenChangeDetector(
    private val eventEmitter: NavigationEventEmitter,
    private val emissionPolicy: NavigationEmissionPolicy
) {

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
        tryEmitIfChanged(shouldEmit = emissionPolicy.emitOnResumed)
    }

    fun onActivityPaused(activity: Activity) {
        val name = ScreenNameDescriptor.getName(activity)
        previouslyLastResumedActivityName = name
        if (lastResumedActivityName == name) {
            lastResumedActivityName = null
        }
        tryEmitIfChanged(shouldEmit = emissionPolicy.emitOnPaused)
    }

    fun onFragmentResumed(fragment: Fragment) {
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        if (fragment is DialogFragment) {
            previouslyLastResumedFragmentName = lastResumedFragmentName
        }

        val name = ScreenNameDescriptor.getName(fragment)
        lastResumedFragmentName = name
        tryEmitIfChanged(shouldEmit = emissionPolicy.emitOnResumed)
    }

    fun onFragmentPaused(fragment: Fragment) {
        if (ScreenNameDescriptor.isIgnored(fragment)) return

        if (fragment is DialogFragment) {
            lastResumedFragmentName = previouslyLastResumedFragmentName
        } else if (lastResumedFragmentName == ScreenNameDescriptor.getName(fragment)) {
            lastResumedFragmentName = null
        }

        previouslyLastResumedFragmentName = ScreenNameDescriptor.getName(fragment)
        tryEmitIfChanged(shouldEmit = emissionPolicy.emitOnPaused)
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
     * Updates internal state and optionally emits a navigation event.
     * When [shouldEmit] is false (e.g. paused callback with ON_SCREEN_ACTIVE policy), we do not
     * emit and do not advance [lastEmittedScreenName], so the next resumed callback still
     * has the correct previous screen.
     */
    private fun tryEmitIfChanged(shouldEmit: Boolean) {
        val current = currentVisibleScreenName() ?: return
        if (current == lastEmittedScreenName) return
        if (!shouldEmit) return

        val previous = lastEmittedScreenName
        lastEmittedScreenName = current
        eventEmitter.emitNavigationEvent(current, previous)
    }
}
