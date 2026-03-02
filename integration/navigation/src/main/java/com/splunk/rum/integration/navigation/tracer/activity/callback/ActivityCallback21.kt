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
import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.navigation.tracer.activity.ActivityTracerManager

internal class ActivityCallback21(override val tracer: ActivityTracerManager) : ActivityCallback {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityCallback21", "onActivityCreated")
        tracer.startActivityCreation(activity)
            .addEvent(GlobalRumConstants.NAVIGATION_ACTIVITY_CREATED_EVENT)
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.d("ActivityCallback21", "onActivityStarted")
        tracer.initiateRestartSpanIfNecessary(activity)
            .addEvent(GlobalRumConstants.NAVIGATION_ACTIVITY_STARTED_EVENT)
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.d("ActivityCallback21", "onActivityResumed")
        tracer.startSpanIfNoneInProgress(activity, GlobalRumConstants.NAVIGATION_RESUMED_SPAN_NAME)
            .addEvent(GlobalRumConstants.NAVIGATION_ACTIVITY_RESUMED_EVENT)
            .endSpanForActivityResumed()
    }

    override fun onActivityPaused(activity: Activity) {
        Logger.d("ActivityCallback21", "onActivityPaused")
        tracer.startSpanIfNoneInProgress(activity, GlobalRumConstants.NAVIGATION_PAUSED_SPAN_NAME)
            .addEvent(GlobalRumConstants.NAVIGATION_ACTIVITY_PAUSED_EVENT)
            .endActiveSpan()
    }

    override fun onActivityStopped(activity: Activity) {
        Logger.d("ActivityCallback21", "onActivityStopped")
        tracer.startSpanIfNoneInProgress(activity, GlobalRumConstants.NAVIGATION_STOPPED_SPAN_NAME)
            .addEvent(GlobalRumConstants.NAVIGATION_ACTIVITY_STOPPED_EVENT)
            .endActiveSpan()
    }

    override fun onActivityDestroyed(activity: Activity) {
        Logger.d("ActivityCallback21", "onActivityDestroyed")
        tracer.startSpanIfNoneInProgress(activity, GlobalRumConstants.NAVIGATION_DESTROYED_SPAN_NAME)
            .addEvent(GlobalRumConstants.NAVIGATION_ACTIVITY_DESTROYED_EVENT)
            .endActiveSpan()
    }
}
