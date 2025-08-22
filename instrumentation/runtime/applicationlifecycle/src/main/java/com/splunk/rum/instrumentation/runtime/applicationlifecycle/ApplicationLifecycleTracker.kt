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

package com.splunk.rum.instrumentation.runtime.applicationlifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.adapters.ActivityLifecycleCallbacksAdapter
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.rum.instrumentation.runtime.applicationlifecycle.model.AppState
import com.splunk.rum.instrumentation.runtime.applicationlifecycle.model.ApplicationLifecycleData

object ApplicationLifecycleTracker {

    private const val TAG = "ApplicationLifecycleTracker"

    val listeners: MutableList<Listener> = arrayListOf()

    private var resumedActivityCount = 0
    private var isAppInForeground = false
    private var isAppCreated = false
    private val backgroundTransitionHandler = Handler(Looper.getMainLooper())

    /**
     * The delay in milliseconds the tracker waits after all activities pause to confirm
     * the application has truly moved to the `BACKGROUND` state. This prevents false
     * background events during rapid activity transitions within the app.
     */
    private const val BACKGROUND_TRANSITION_DELAY_MS = 500L

    internal fun onCreate(application: Application) {
        Logger.d(TAG, "onCreate() called")
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacksAdapter {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (!isAppCreated) {
                isAppCreated = true
                Logger.d(TAG, "First activity created: ${activity.javaClass.simpleName}. Notifying AppState.CREATED.")
                notifyListeners(ApplicationLifecycleData(System.currentTimeMillis(), AppState.CREATED))
            }
        }

        override fun onActivityResumed(activity: Activity) {
            backgroundTransitionHandler.removeCallbacks(backgroundTransitionRunnable)

            if (resumedActivityCount == 0) {
                isAppInForeground = true
                Logger.d(TAG, "App moved to foreground. Notifying AppState.FOREGROUND.")
                notifyListeners(ApplicationLifecycleData(System.currentTimeMillis(), AppState.FOREGROUND))
            }
            resumedActivityCount++
        }

        override fun onActivityPaused(activity: Activity) {
            resumedActivityCount--

            if (resumedActivityCount == 0) {
                Logger.d(
                    TAG,
                    "All activities paused. Scheduling background transition check in $BACKGROUND_TRANSITION_DELAY_MS ms."
                )
                backgroundTransitionHandler.postDelayed(backgroundTransitionRunnable, BACKGROUND_TRANSITION_DELAY_MS)
            }
        }

        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityPreStarted(activity: Activity) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    private val backgroundTransitionRunnable = Runnable {
        if (resumedActivityCount == 0 && isAppInForeground) {
            isAppInForeground = false
            Logger.d(TAG, "Background transition confirmed. Notifying AppState.BACKGROUND.")
            notifyListeners(ApplicationLifecycleData(System.currentTimeMillis(), AppState.BACKGROUND))
        } else {
            Logger.d(
                TAG,
                "Background transition check ran, but app is no longer in background state (resumedActivityCount: $resumedActivityCount, isAppInForeground: $isAppInForeground)."
            )
        }
    }

    interface Listener {
        fun onApplicationLifecycleChange(applicationLifecycleData: ApplicationLifecycleData)
    }

    private fun notifyListeners(data: ApplicationLifecycleData) {
        listeners.forEachFast { listener ->
            listener.onApplicationLifecycleChange(data)
        }
    }
}
