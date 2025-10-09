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
import com.splunk.rum.integration.navigation.tracer.activity.ActivityTracerManager

internal class ActivityCallback29(override val tracer: ActivityTracerManager) : ActivityCallback {

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        tracer.startActivityCreation(activity).addEvent("activityPreCreated")
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        tracer.addEvent(activity, "activityCreated")
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        tracer.addEvent(activity, "activityPostCreated")
    }

    override fun onActivityPreStarted(activity: Activity) {
        tracer.initiateRestartSpanIfNecessary(activity).addEvent("activityPreStarted")
    }

    override fun onActivityStarted(activity: Activity) {
        tracer.addEvent(activity, "activityStarted")
    }

    override fun onActivityPostStarted(activity: Activity) {
        tracer.addEvent(activity, "activityPostStarted")
    }

    override fun onActivityPreResumed(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Resumed").addEvent("activityPreResumed")
    }

    override fun onActivityResumed(activity: Activity) {
        tracer.addEvent(activity, "onActivityResumed")
    }

    override fun onActivityPostResumed(activity: Activity) {
        tracer
            .addEvent(activity, "activityPostResumed")
            .endSpanForActivityResumed()
    }

    override fun onActivityPrePaused(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Paused").addEvent("activityPrePaused")
    }

    override fun onActivityPaused(activity: Activity) {
        tracer.addEvent(activity, "onActivityPaused")
    }

    override fun onActivityPostPaused(activity: Activity) {
        tracer.addEvent(activity, "activityPostPaused").endActiveSpan()
    }

    override fun onActivityPreStopped(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Stopped").addEvent("activityPreStopped")
    }

    override fun onActivityStopped(activity: Activity) {
        tracer.addEvent(activity, "activityStopped")
    }

    override fun onActivityPostStopped(activity: Activity) {
        tracer.addEvent(activity, "activityPostStopped").endActiveSpan()
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Destroyed").addEvent("activityPreDestroyed")
    }

    override fun onActivityDestroyed(activity: Activity) {
        tracer.addEvent(activity, "activityDestroyed")
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        tracer.addEvent(activity, "activityPostDestroyed").endActiveSpan()
    }
}
