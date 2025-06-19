/*
 * Copyright 2024 Splunk Inc.
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
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class SplunkInternalGlobalAttributeSpanProcessor : SpanProcessor {

    init {
        /**
         * Having a value for the screen.name attribute is mandatory for metrics to be derived on the platform.
         * Using [RumConstants.DEFAULT_SCREEN_NAME] ensures that a screen.name is always present.
         */
        attributes[RumConstants.SCREEN_NAME_KEY] = RumConstants.DEFAULT_SCREEN_NAME
    }

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        attributes.forEach { key, value ->
            @Suppress("UNCHECKED_CAST")
            span.setAttribute(key as AttributeKey<Any>, value)
        }
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {
    }

    override fun isEndRequired(): Boolean = true

    companion object {
        val attributes = MutableAttributes()
    }
}
