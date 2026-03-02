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
import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.navigation.tracer.activity.ActivityTracerManager

internal class ActivityCallback29(override val tracer: ActivityTracerManager) : ActivityCallback {

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityCallback29", "onActivityPreCreated")
        tracer.startActivityCreation(activity).addEvent(RumConstants.NAVIGATION_ACTIVITY_PRE_CREATED_EVENT)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityCallback29", "onActivityCreated")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_CREATED_EVENT)
    }

    override fun onActivityPostCreated(activity: Activity, savedInstanceState: Bundle?) {
        Logger.d("ActivityCallback29", "onActivityPostCreated")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_POST_CREATED_EVENT)
    }

    override fun onActivityPreStarted(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPreStarted")
        tracer.initiateRestartSpanIfNecessary(activity).addEvent(RumConstants.NAVIGATION_ACTIVITY_PRE_STARTED_EVENT)
    }

    override fun onActivityStarted(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityStarted")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_STARTED_EVENT)
    }

    override fun onActivityPostStarted(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPostStarted")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_POST_STARTED_EVENT)
    }

    override fun onActivityPreResumed(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPreResumed")
        tracer.startSpanIfNoneInProgress(activity, RumConstants.NAVIGATION_RESUMED_SPAN_NAME)
            .addEvent(RumConstants.NAVIGATION_ACTIVITY_PRE_RESUMED_EVENT)
    }

    override fun onActivityResumed(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityResumed")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_RESUMED_EVENT)
    }

    override fun onActivityPostResumed(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPostResumed")
        tracer
            .addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_POST_RESUMED_EVENT)
            .endSpanForActivityResumed()
    }

    override fun onActivityPrePaused(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPrePaused")
        tracer.startSpanIfNoneInProgress(activity, RumConstants.NAVIGATION_PAUSED_SPAN_NAME)
            .addEvent(RumConstants.NAVIGATION_ACTIVITY_PRE_PAUSED_EVENT)
    }

    override fun onActivityPaused(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPaused")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_PAUSED_EVENT)
    }

    override fun onActivityPostPaused(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPostPaused")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_POST_PAUSED_EVENT).endActiveSpan()
    }

    override fun onActivityPreStopped(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPreStopped")
        tracer.startSpanIfNoneInProgress(activity, RumConstants.NAVIGATION_STOPPED_SPAN_NAME)
            .addEvent(RumConstants.NAVIGATION_ACTIVITY_PRE_STOPPED_EVENT)
    }

    override fun onActivityStopped(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityStopped")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_STOPPED_EVENT)
    }

    override fun onActivityPostStopped(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPostStopped")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_POST_STOPPED_EVENT).endActiveSpan()
    }

    override fun onActivityPreDestroyed(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPreDestroyed")
        tracer.startSpanIfNoneInProgress(activity, RumConstants.NAVIGATION_DESTROYED_SPAN_NAME)
            .addEvent(RumConstants.NAVIGATION_ACTIVITY_PRE_DESTROYED_EVENT)
    }

    override fun onActivityDestroyed(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityDestroyed")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_DESTROYED_EVENT)
    }

    override fun onActivityPostDestroyed(activity: Activity) {
        Logger.d("ActivityCallback29", "onActivityPostDestroyed")
        tracer.addEvent(activity, RumConstants.NAVIGATION_ACTIVITY_POST_DESTROYED_EVENT).endActiveSpan()
    }
}
