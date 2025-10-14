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

package com.splunk.rum.integration.navigation.screen

import android.app.Activity
import android.app.Application
import android.os.Build
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.splunk.rum.integration.navigation.descriptor.ScreenNameDescriptor
import com.splunk.rum.integration.navigation.screen.activity.ActivityCallback21
import com.splunk.rum.integration.navigation.screen.activity.ActivityCallback29
import com.splunk.rum.integration.navigation.tracer.fragment.activity.FragmentActivityCallback21
import com.splunk.rum.integration.navigation.tracer.fragment.activity.FragmentActivityCallback29

internal class VisibleScreenTracker(application: Application) {

    private var lastResumedActivity: String? = null
    private var previouslyLastResumedActivity: String? = null
    private var lastResumedFragment: String? = null
    private var previouslyLastResumedFragment: String? = null

    init {
        observeActivities(application)
        observeFragments(application)
    }

    fun onActivityResumed(activity: Activity) {
        lastResumedActivity = activity.javaClass.simpleName
    }

    fun onActivityPaused(activity: Activity) {
        previouslyLastResumedActivity = activity.javaClass.simpleName

        if (lastResumedActivity == activity.javaClass.simpleName) {
            lastResumedActivity = null
        }
    }

    fun onFragmentResumed(fragment: Fragment) {
        if (ScreenNameDescriptor.isIgnored(fragment)) {
            return
        }

        if (fragment is DialogFragment) {
            previouslyLastResumedFragment = lastResumedFragment
        }

        lastResumedFragment = fragment.javaClass.simpleName
    }

    fun onFragmentPaused(fragment: Fragment) {
        if (ScreenNameDescriptor.isIgnored(fragment)) {
            return
        }

        if (fragment is DialogFragment) {
            lastResumedFragment = previouslyLastResumedFragment
        } else if (lastResumedFragment == fragment.javaClass.simpleName) {
            lastResumedFragment = null
        }

        previouslyLastResumedFragment = fragment.javaClass.simpleName
    }

    private fun observeActivities(application: Application) {
        val callback = if (Build.VERSION.SDK_INT >= 29) {
            ActivityCallback29(this)
        } else {
            ActivityCallback21(this)
        }

        application.registerActivityLifecycleCallbacks(callback)
    }

    private fun observeFragments(application: Application) {
        val fragmentLifecycle = VisibleFragmentTracker(this)

        val callback = if (Build.VERSION.SDK_INT >= 29) {
            FragmentActivityCallback29(fragmentLifecycle)
        } else {
            FragmentActivityCallback21(fragmentLifecycle)
        }

        application.registerActivityLifecycleCallbacks(callback)
    }
}
