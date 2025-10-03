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

package com.splunk.rum.startup

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.adapters.ActivityLifecycleCallbacksAdapter
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.android.common.utils.extensions.rootView

object ApplicationStartupTimekeeper {

    private const val TAG = "ApplicationStartupTimekeeper"

    private var firstTimestamp = 0L
    private var firstElapsed = 0L

    private var isColdStartCompleted = false

    var isEnabled = true

    val listeners: MutableList<Listener> = arrayListOf()

    internal fun onInit() {
        firstTimestamp = System.currentTimeMillis()
        firstElapsed = SystemClock.elapsedRealtime()
    }

    internal fun onCreate(application: Application) {
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private val activityLifecycleCallbacks = object : ActivityLifecycleCallbacksAdapter {

        private var createdActivityCount = 0
        private var startedActivityCount = 0
        private var resumedActivityCount = 0

        private var firstActivityCreateTimestamp = 0L
        private var firstActivityCreateElapsed = 0L
        private var isWarmStartPending = false

        private var firstActivityStartTimestamp = 0L
        private var firstActivityStartElapsed = 0L
        private var isHotStartPending = false

        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            createdActivityCount++

            if (isColdStartCompleted && createdActivityCount == 1) {
                firstActivityCreateTimestamp = System.currentTimeMillis()
                firstActivityCreateElapsed = SystemClock.elapsedRealtime()
                isWarmStartPending = true
            }
        }

        override fun onActivityPreStarted(activity: Activity) {
            startedActivityCount++

            if (isColdStartCompleted && !isWarmStartPending && !isHotStartPending && startedActivityCount == 1) {
                firstActivityStartTimestamp = System.currentTimeMillis()
                firstActivityStartElapsed = SystemClock.elapsedRealtime()
                isHotStartPending = true
            }
        }

        override fun onActivityResumed(activity: Activity) {
            resumedActivityCount++

            if (isEnabled &&
                resumedActivityCount == 1 &&
                (!isColdStartCompleted || isHotStartPending || isWarmStartPending)
            ) {
                val rootView = activity.rootView

                if (rootView == null) {
                    Logger.w(TAG, "Activity's rootView not found.")
                    return
                }

                rootView.doOnDraw {
                    if (!isEnabled) {
                        return@doOnDraw
                    }

                    val reporter: Listener.(Long, Long, Long) -> Unit

                    val startTimestamp: Long
                    val endTimestamp: Long
                    val duration: Long

                    when {
                        !isColdStartCompleted -> {
                            reporter = Listener::onColdStarted

                            startTimestamp = firstTimestamp
                            duration = SystemClock.elapsedRealtime() - firstElapsed
                            endTimestamp = firstTimestamp + duration

                            isColdStartCompleted = true
                        }
                        isHotStartPending -> {
                            reporter = Listener::onHotStarted

                            startTimestamp = firstActivityStartTimestamp
                            duration = SystemClock.elapsedRealtime() - firstActivityStartElapsed
                            endTimestamp = firstActivityStartTimestamp + duration

                            isHotStartPending = false
                        }
                        isWarmStartPending -> {
                            reporter = Listener::onWarmStarted

                            startTimestamp = firstActivityCreateTimestamp
                            duration = SystemClock.elapsedRealtime() - firstActivityCreateElapsed
                            endTimestamp = firstActivityCreateTimestamp + duration

                            isWarmStartPending = false
                        }
                        else ->
                            return@doOnDraw
                    }

                    listeners.forEachFast {
                        reporter(it, startTimestamp, endTimestamp, duration)
                    }
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

    // FIXME Compiler error. Use com.splunk.android.common.utils.extensions.doOnDraw once the issue is fixed.
    private inline fun View.doOnDraw(crossinline action: () -> Unit) {
        var pendingRemove = false

        val onDrawListener = ViewTreeObserver.OnDrawListener {
            pendingRemove = true
            action()
        }

        val onPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (pendingRemove && viewTreeObserver.isAlive) {
                    rootView.viewTreeObserver.removeOnDrawListener(onDrawListener)
                    rootView.viewTreeObserver.removeOnPreDrawListener(this)
                }

                return true
            }
        }

        viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
        viewTreeObserver.addOnDrawListener(onDrawListener)
    }

    interface Listener {

        /**
         * The application is launched from a completely inactive state.
         * Kill the app > press the application icon.
         */
        fun onColdStarted(startTimestamp: Long, endTimestamp: Long, duration: Long)

        /**
         * The application is launched after being recently closed or moved to the background, but still resides in memory.
         * Open the app > press back button > press the app icon.
         */
        fun onWarmStarted(startTimestamp: Long, endTimestamp: Long, duration: Long)

        /**
         * The application is already running in the background and is brought to the foreground.
         * Open the app > press home button > press the app icon.
         */
        fun onHotStarted(startTimestamp: Long, endTimestamp: Long, duration: Long)
    }
}
