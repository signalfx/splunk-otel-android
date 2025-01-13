/*
 * Copyright 2024 Splunk Inc.
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

package com.cisco.android.rum.startup

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.smartlook.sdk.common.utils.adapters.ActivityLifecycleCallbacksAdapter
import com.smartlook.sdk.common.utils.extensions.forEachFast

object ApplicationStartupTimekeeper {

    private val handler = Handler(Looper.getMainLooper())

    private var firstTimestamp = 0L
    private var isColdStartCompleted = false

    var isEnabled = true

    val listeners: MutableList<Listener> = arrayListOf()

    internal fun onInit() {
        firstTimestamp = System.currentTimeMillis()
    }

    internal fun onCreate(application: Application) {
        handler.twoConsecutivePosts {
            isColdStartCompleted = true

            if (isEnabled) {
                val duration = System.currentTimeMillis() - firstTimestamp
                listeners.forEachFast { it.onColdStarted(duration) }
            }
        }

        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacksAdapter {

        private var createdActivityCount = 0
        private var startedActivityCount = 0
        private var resumedActivityCount = 0

        private var firstActivityCreateTimestamp = 0L
        private var isWarmStartPending = false

        private var firstActivityStartTimestamp = 0L
        private var isHotStartPending = false

        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            createdActivityCount++

            if (isColdStartCompleted && createdActivityCount == 1) {
                firstActivityCreateTimestamp = System.currentTimeMillis()
                isWarmStartPending = true
            }
        }

        override fun onActivityPreStarted(activity: Activity) {
            startedActivityCount++

            if (isColdStartCompleted && !isWarmStartPending && !isHotStartPending) {
                firstActivityStartTimestamp = System.currentTimeMillis()
                isHotStartPending = true
            }
        }

        override fun onActivityResumed(activity: Activity) {
            resumedActivityCount++

            if (resumedActivityCount == 1 && (isHotStartPending || isWarmStartPending))
                handler.twoConsecutivePosts {
                    if (isHotStartPending) {
                        if (isEnabled) {
                            val duration = System.currentTimeMillis() - firstActivityStartTimestamp
                            listeners.forEachFast { it.onHotStarted(duration) }
                        }

                        isHotStartPending = false
                    }

                    if (isWarmStartPending) {
                        if (isEnabled) {
                            val duration = System.currentTimeMillis() - firstActivityCreateTimestamp
                            listeners.forEachFast { it.onWarmStarted(duration) }
                        }

                        isWarmStartPending = false
                    }
                }
        }

        override fun onActivityPaused(activity: Activity) {
            resumedActivityCount--
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivityCount--
        }

        override fun onActivityDestroyed(activity: Activity) {
            createdActivityCount--
        }
    }

    private fun Handler.twoConsecutivePosts(action: () -> Unit) {
        post {
            post(action)
        }
    }

    interface Listener {

        /**
         * The application is launched from a completely inactive state.
         * Kill the app > press the application icon.
         */
        fun onColdStarted(duration: Long)

        /**
         * The application is launched after being recently closed or moved to the background, but still resides in memory.
         * Open the app > press back button > press the app icon.
         */
        fun onWarmStarted(duration: Long)

        /**
         * The application is already running in the background and is brought to the foreground.
         * Open the app > press home button > press the app icon.
         */
        fun onHotStarted(duration: Long)
    }
}
