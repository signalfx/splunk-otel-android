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
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.MutableListObserver
import com.splunk.android.common.utils.adapters.ActivityLifecycleCallbacksAdapter
import com.splunk.android.common.utils.extensions.forEachFast
import com.splunk.android.common.utils.extensions.rootView
import com.splunk.rum.startup.extension.doOnDraw
import com.splunk.rum.startup.model.StartEvent
import com.splunk.rum.startup.util.ProcessInfo
import com.splunk.rum.utils.extensions.isStartedInForeground

object ApplicationStartupTimekeeper {

    private const val TAG = "ApplicationStartupTimekeeper"

    private val pendingEvents = mutableListOf<StartEvent>()

    private var isColdStartCompleted = false

    var isEnabled = true

    val listeners: MutableList<Listener> = MutableListObserver<Listener>(arrayListOf(), ListenerObserver())

    internal fun onCreate(application: Application) {
        isColdStartCompleted = !application.isStartedInForeground
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                onActivityPreCreatedCompat()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                onActivityPreCreatedCompat()
            }
        }

        private fun onActivityPreCreatedCompat() {
            createdActivityCount++

            if (isColdStartCompleted && createdActivityCount == 1) {
                firstActivityCreateTimestamp = System.currentTimeMillis()
                firstActivityCreateElapsed = SystemClock.elapsedRealtime()
                isWarmStartPending = true
            }
        }

        override fun onActivityPreStarted(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                onActivityPreStartedCompat()
            }
        }

        override fun onActivityStarted(activity: Activity) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                onActivityPreStartedCompat()
            }
        }

        private fun onActivityPreStartedCompat() {
            startedActivityCount++

            if (isColdStartCompleted && !isWarmStartPending && !isHotStartPending && startedActivityCount == 1) {
                firstActivityStartTimestamp = System.currentTimeMillis()
                firstActivityStartElapsed = SystemClock.elapsedRealtime()
                isHotStartPending = true
            }
        }

        override fun onActivityResumed(activity: Activity) {
            resumedActivityCount++

            if (isEnabled && resumedActivityCount == 1 &&
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

                    processStart()
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

        private fun processStart() {
            val startType: StartEvent.Type
            val startTimestamp: Long
            val endTimestamp: Long

            when {
                !isColdStartCompleted -> {
                    startType = StartEvent.Type.COLD

                    val duration = SystemClock.uptimeMillis() - ProcessInfo.getStartUptimeMillis()
                    endTimestamp = System.currentTimeMillis()
                    startTimestamp = endTimestamp - duration

                    isColdStartCompleted = true
                }
                isHotStartPending -> {
                    startType = StartEvent.Type.HOT

                    startTimestamp = firstActivityStartTimestamp
                    val duration = SystemClock.elapsedRealtime() - firstActivityStartElapsed
                    endTimestamp = firstActivityStartTimestamp + duration

                    isHotStartPending = false
                }
                isWarmStartPending -> {
                    startType = StartEvent.Type.WARM

                    startTimestamp = firstActivityCreateTimestamp
                    val duration = SystemClock.elapsedRealtime() - firstActivityCreateElapsed
                    endTimestamp = firstActivityCreateTimestamp + duration

                    isWarmStartPending = false
                }
                else ->
                    return
            }

            val event = StartEvent(startType, startTimestamp, endTimestamp)

            if (listeners.isEmpty()) {
                Logger.d(TAG, "processStart() - No listeners, caching event $event")
                pendingEvents += event
            } else {
                listeners.forEachFast { it.onStartEvent(event) }
            }
        }
    }

    interface Listener {
        fun onStartEvent(event: StartEvent)
    }

    private class ListenerObserver : MutableListObserver.Observer<Listener> {
        override fun onAdded(element: Listener) {
            if (listeners.isEmpty()) {
                pendingEvents.forEachFast {
                    Logger.d(TAG, "onAdded() - Delivering pending event of type ${it.type.name}")
                    element.onStartEvent(it)
                }

                pendingEvents.clear()
            }
        }
    }
}
