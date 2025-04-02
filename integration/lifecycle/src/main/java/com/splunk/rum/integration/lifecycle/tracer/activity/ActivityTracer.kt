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

import com.splunk.rum.integration.lifecycle.tracer.ActiveSpan
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer

internal class ActivityTracer(
    private var initialAppActivity: String?,
    private val tracer: Tracer,
    private val activityName: String,
    private val screenName: String,
    private val activeSpan: ActiveSpan
) {

    fun startSpanIfNoneInProgress(spanName: String): ActivityTracer {
        if (activeSpan.isSpanInProgress())
            return this

        activeSpan.startSpan { createSpan(spanName) }
        return this
    }

    fun startActivityCreation(): ActivityTracer {
        activeSpan.startSpan { createSpan("Created") }
        return this
    }

    fun initiateRestartSpanIfNecessary(): ActivityTracer {
        if (activeSpan.isSpanInProgress())
            return this

        activeSpan.startSpan { createSpan("Restarted") }
        return this
    }

    fun endSpanForActivityResumed() {
        if (initialAppActivity == null)
            initialAppActivity = activityName

        endActiveSpan()
    }

    fun endActiveSpan() {
        activeSpan.endActiveSpan()
    }

    fun addPreviousScreenAttribute(): ActivityTracer {
        activeSpan.addPreviousScreenAttribute(activityName)
        return this
    }

    fun addEvent(eventName: String): ActivityTracer {
        activeSpan.addEvent(eventName)
        return this
    }

    private fun createSpan(spanName: String): Span {
        val spanBuilder = tracer.spanBuilder(spanName)
            .setAttribute(ACTIVITY_NAME_KEY, activityName)

        return spanBuilder.startSpan()
            .setAttribute(SCREEN_NAME_KEY, screenName)
    }

    private companion object {
        val ACTIVITY_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("activityName")
        val SCREEN_NAME_KEY: AttributeKey<String> = AttributeKey.stringKey("screen.name")
    }
}
