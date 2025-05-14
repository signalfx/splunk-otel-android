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

package com.splunk.rum.integration.lifecycle.tracer.activity

import android.app.Activity
import com.splunk.rum.integration.lifecycle.descriptor.ScreenNameDescriptor
import com.splunk.rum.integration.lifecycle.screen.VisibleScreenTracker
import com.splunk.rum.integration.lifecycle.tracer.ActiveSpan
import io.opentelemetry.api.trace.Tracer

internal class ActivityTracerManager(
    private val tracer: Tracer,
    private val visibleScreenTracker: VisibleScreenTracker,
    private val initialAppActivity: String?
) {

    private val tracers = HashMap<String, ActivityTracer>()

    fun addEvent(activity: Activity, eventName: String): ActivityTracer = getTracer(activity).addEvent(eventName)

    fun startSpanIfNoneInProgress(activity: Activity, spanName: String): ActivityTracer = getTracer(activity).startSpanIfNoneInProgress(spanName)

    fun initiateRestartSpanIfNecessary(activity: Activity): ActivityTracer = getTracer(activity).initiateRestartSpanIfNecessary()

    fun startActivityCreation(activity: Activity): ActivityTracer = getTracer(activity).startActivityCreation()

    private fun getTracer(activity: Activity): ActivityTracer {
        val className = activity::class.java.name
        var activityTracer = tracers[className]

        if (activityTracer == null) {
            activityTracer = ActivityTracer(
                initialAppActivity = initialAppActivity,
                tracer = tracer,
                activityName = activity::class.java.simpleName,
                screenName = ScreenNameDescriptor.getName(activity),
                activeSpan = ActiveSpan(visibleScreenTracker::previouslyVisibleScreen)
            )

            tracers[className] = activityTracer
        }

        return activityTracer
    }
}
