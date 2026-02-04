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

package com.splunk.rum.integration.lifecycle.callback

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.splunk.android.common.logger.Logger

/**
 * Registers fragment lifecycle callbacks on FragmentActivity instances (API 29+).
 * Pattern copied from: navigation/tracer/fragment/activity/FragmentActivityCallback29.kt
 */
@RequiresApi(29)
internal class FragmentActivityCallback29(private val fragmentCallback: FragmentLifecycleCallback) :
    Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity) {
            Logger.d(
                "FragmentActivityCallback29",
                "Registering fragment callbacks for: ${activity::class.java.simpleName}"
            )
            // CRITICAL: recursive = true to capture nested fragments
            activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallback, true)
        }
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
