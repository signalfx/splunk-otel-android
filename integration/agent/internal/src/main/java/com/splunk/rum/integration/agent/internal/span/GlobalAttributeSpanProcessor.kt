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

package com.splunk.rum.integration.agent.internal.span

import com.cisco.android.common.utils.extensions.forEachFast
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class GlobalAttributeSpanProcessor : SpanProcessor {

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        attributes.forEachFast {
            when (it) {
                is Attribute.Boolean -> span.setAttribute(it.name, it.value)
                is Attribute.Double -> span.setAttribute(it.name, it.value)
                is Attribute.Long -> span.setAttribute(it.name, it.value)
                is Attribute.String -> span.setAttribute(it.name, it.value)
            }
        }
    }

    override fun isStartRequired(): Boolean {
        return true
    }

    override fun onEnd(span: ReadableSpan) {
    }

    override fun isEndRequired(): Boolean {
        return true
    }

    sealed interface Attribute {

        val name: kotlin.String

        data class Boolean(override val name: kotlin.String, val value: kotlin.Boolean) : Attribute
        data class Double(override val name: kotlin.String, val value: kotlin.Double) : Attribute
        data class String(override val name: kotlin.String, val value: kotlin.String) : Attribute
        data class Long(override val name: kotlin.String, val value: kotlin.Long) : Attribute
    }

    companion object {
        val attributes: MutableList<Attribute> = ArrayList()
    }
}
