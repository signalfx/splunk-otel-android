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

package com.splunk.rum.integration.agent.internal.processor

import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.integration.agent.internal.attributes.IScreenNameTracker
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class LastScreenNameSpanProcessor(private val screeNameTracker: IScreenNameTracker) : SpanProcessor {

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        if (span.name == RumConstants.NAVIGATION_NAME || span.name == RumConstants.APP_START_NAME) {
            screeNameTracker.lastScreenName?.let { span.setAttribute(RumConstants.LAST_SCREEN_NAME_KEY, it) }
        }
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {
    }

    override fun isEndRequired(): Boolean = false
}
