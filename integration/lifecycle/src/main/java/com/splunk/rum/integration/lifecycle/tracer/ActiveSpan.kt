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

package com.splunk.rum.integration.lifecycle.tracer

import com.splunk.rum.common.otel.internal.RumConstants
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope

internal class ActiveSpan(private val lastVisibleScreenProvider: () -> String?) {

    private var span: Span? = null
    private var scope: Scope? = null

    fun isSpanInProgress(): Boolean = span != null

    fun startSpan(spanCreator: () -> Span?) {
        if (span != null) {
            return
        }

        span = spanCreator()
        scope = span?.makeCurrent()
    }

    fun endActiveSpan() {
        scope?.close()
        scope = null

        span?.end()
        span = null
    }

    fun addEvent(eventName: String) {
        span?.addEvent(eventName)
    }

    fun addPreviousScreenAttribute(screenName: String) {
        val span = span ?: return

        val previouslyVisibleScreen = lastVisibleScreenProvider()
        if (previouslyVisibleScreen != null && screenName != previouslyVisibleScreen) {
            span.setAttribute(RumConstants.LAST_SCREEN_NAME_KEY, previouslyVisibleScreen)
        }
    }
}
