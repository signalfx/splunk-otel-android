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

package com.splunk.rum.integration.navigation.tracer.fragment

import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.navigation.tracer.ActiveSpan
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer

internal class FragmentTracer(
    private val tracer: Tracer,
    private val fragmentName: String,
    private val screenName: String,
    private val activeSpan: ActiveSpan
) {

    fun startSpanIfNoneInProgress(action: String): FragmentTracer {
        if (activeSpan.isSpanInProgress()) {
            return this
        }

        activeSpan.startSpan { createSpan(action) }
        return this
    }

    fun startFragmentCreation(): FragmentTracer {
        activeSpan.startSpan { createSpan("Created") }
        return this
    }

    fun endActiveSpan() {
        activeSpan.endActiveSpan()
    }

    fun addPreviousScreenAttribute(): FragmentTracer {
        activeSpan.addPreviousScreenAttribute(fragmentName)
        return this
    }

    fun addEvent(eventName: String): FragmentTracer {
        activeSpan.addEvent(eventName)
        return this
    }

    private fun createSpan(spanName: String): Span {
        val span = tracer.spanBuilder(spanName)
            .setAttribute(FRAGMENT_NAME_KEY, fragmentName)
            .setAttribute(RumConstants.COMPONENT_KEY, "ui")
            .startSpan()

        span.setAttribute(RumConstants.SCREEN_NAME_KEY, screenName)
        return span
    }

    private companion object {
        val FRAGMENT_NAME_KEY: AttributeKey<String?> = AttributeKey.stringKey("fragment.name")
    }
}
