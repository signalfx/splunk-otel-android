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

import com.splunk.rum.agent.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.common.attributes.MutableAttributes
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class SplunkInternalGlobalAttributeSpanProcessor : SpanProcessor {

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        val isNetworkEvent = span.name == NETWORK_CHANGE_EVENT_NAME
        attributes.forEach { key, value ->
            // screen.name is added onto every log record at emit time by
            // ScreenNameLogRecordProcessor. When those log records are later converted to
            // spans the attribute is already present. Overwriting it here with the current
            // global value would be incorrect because ScreenNameTracker may have advanced
            // to a different screen between emit and batch flush.
            if (key == GlobalRumConstants.SCREEN_NAME_KEY &&
                span.getAttribute(GlobalRumConstants.SCREEN_NAME_KEY) != null
            ) {
                return@forEach
            }
            // Network change logs capture the complete network state when they are emitted.
            // They can be converted to spans after the global network state has changed, so
            // do not mix that later state into the event-time snapshot.
            if (isNetworkEvent && key in NETWORK_ATTRIBUTE_KEYS) {
                return@forEach
            }
            @Suppress("UNCHECKED_CAST")
            span.setAttribute(key as AttributeKey<Any>, value)
        }
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) {
    }

    override fun isEndRequired(): Boolean = true

    companion object {
        private const val NETWORK_CHANGE_EVENT_NAME = "network.change"
        private val NETWORK_ATTRIBUTE_KEYS = setOf(
            AttributeKey.stringKey("network.connection.type"),
            AttributeKey.stringKey("network.connection.subtype"),
            AttributeKey.stringKey("network.carrier.name"),
            AttributeKey.stringKey("network.carrier.mcc"),
            AttributeKey.stringKey("network.carrier.mnc"),
            AttributeKey.stringKey("network.carrier.icc")
        )

        val attributes = MutableAttributes().apply {
            this[GlobalRumConstants.SCREEN_NAME_KEY] = GlobalRumConstants.DEFAULT_SCREEN_NAME
        }
    }
}
