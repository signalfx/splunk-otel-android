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

package com.splunk.rum.integration.lifecycle.tracer.activity.callback

import android.app.Activity
import android.os.Bundle
import com.splunk.rum.integration.lifecycle.tracer.activity.ActivityTracerManager

internal class ActivityCallback21(override val tracer: ActivityTracerManager) : ActivityCallback {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        tracer.startActivityCreation(activity)
            .addEvent("activityCreated")
    }

    override fun onActivityStarted(activity: Activity) {
        tracer.initiateRestartSpanIfNecessary(activity)
            .addEvent("activityStarted")
    }

    override fun onActivityResumed(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Resumed")
            .addEvent("onActivityResumed")
            .addPreviousScreenAttribute()
            .endSpanForActivityResumed()
    }

    override fun onActivityPaused(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Paused")
            .addEvent("onActivityPaused")
            .endActiveSpan()
    }

    override fun onActivityStopped(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Stopped")
            .addEvent("activityStopped")
            .endActiveSpan()
    }

    override fun onActivityDestroyed(activity: Activity) {
        tracer.startSpanIfNoneInProgress(activity, "Destroyed")
            .addEvent("activityDestroyed")
            .endActiveSpan()
    }
}
