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

package com.splunk.rum.integration.navigation.tracer.activity.callback

import android.app.Activity
import android.os.Bundle
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.navigation.tracer.activity.ActivityTracerManager

internal class ActivityCallback29(override val tracer: ActivityTracerManager) : ActivityCallback {

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityCallback29",  "onActivityPreCreated")
        tracer.startActivityCreation(activity).addEvent("activityPreCreated")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityCallback29",  "onActivityCreated")
        tracer.addEvent(activity, "activityCreated")
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityCallback29",  "onActivityPostCreated")
        tracer.addEvent(activity, "activityPostCreated")
    }

    override fun onActivityPreStarted(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPreStarted")
        tracer.initiateRestartSpanIfNecessary(activity).addEvent("activityPreStarted")
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityStarted")
        tracer.addEvent(activity, "activityStarted")
    }

    override fun onActivityPostStarted(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPostStarted")
        tracer.addEvent(activity, "activityPostStarted")
    }

    override fun onActivityPreResumed(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPreResumed")
        tracer.startSpanIfNoneInProgress(activity, "Resumed").addEvent("activityPreResumed")
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityResumed")
        tracer.addEvent(activity, "onActivityResumed")
    }

    override fun onActivityPostResumed(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPostResumed")
        tracer
            .addEvent(activity, "activityPostResumed")
            .endSpanForActivityResumed()
    }

    override fun onActivityPrePaused(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPrePaused")
        tracer.startSpanIfNoneInProgress(activity, "Paused").addEvent("activityPrePaused")
    }

    override fun onActivityPaused(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPaused")
        tracer.addEvent(activity, "onActivityPaused")
    }

    override fun onActivityPostPaused(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPostPaused")
        tracer.addEvent(activity, "activityPostPaused").endActiveSpan()
    }

    override fun onActivityPreStopped(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPreStopped")
        tracer.startSpanIfNoneInProgress(activity, "Stopped").addEvent("activityPreStopped")
    }

    override fun onActivityStopped(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityStopped")
        tracer.addEvent(activity, "activityStopped")
    }

    override fun onActivityPostStopped(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPostStopped")
        tracer.addEvent(activity, "activityPostStopped").endActiveSpan()
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPreDestroyed")
        tracer.startSpanIfNoneInProgress(activity, "Destroyed").addEvent("activityPreDestroyed")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityDestroyed")
        tracer.addEvent(activity, "activityDestroyed")
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        Logger.d("ActivityCallback29",  "onActivityPostDestroyed")
        tracer.addEvent(activity, "activityPostDestroyed").endActiveSpan()
    }
}
