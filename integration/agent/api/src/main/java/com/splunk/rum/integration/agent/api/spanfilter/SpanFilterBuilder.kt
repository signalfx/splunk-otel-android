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

@file:Suppress("UNCHECKED_CAST")

package com.splunk.rum.integration.agent.api.spanfilter

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.SpanData

/**
 * Delegating wrapper around otel SpanDataModifier.
 */
@Deprecated("TODO")
class SpanFilterBuilder internal constructor() {
    internal val rejectSpanNames: MutableList<(String) -> Boolean> = mutableListOf()
    internal val rejectSpanAttributes: MutableMap<AttributeKey<*>, (Any) -> Boolean> = mutableMapOf()
    internal val spanAttributeReplacements: MutableMap<AttributeKey<*>, (Any) -> Any?> = mutableMapOf()

    /**
     * Remove matching spans from the exporter pipeline.
     *
     *
     * Spans with names that match the `spanNamePredicate` will not be exported.
     *
     * @param spanNamePredicate A function that returns true if a span with passed name should be
     * rejected.
     * @return `this`.
     */
    fun rejectSpansByName(spanNamePredicate: (String) -> Boolean): SpanFilterBuilder {
        rejectSpanNames.add(spanNamePredicate)
        return this
    }

    /**
     * Remove matching spans from the exporter pipeline.
     *
     *
     * Any span that contains an attribute with key `attributeKey` and value matching the
     * `attributeValuePredicate` will not be exported.
     *
     * @param attributeKey An attribute key to match.
     * @param attributeValuePredicate A function that returns true if a span containing an attribute
     * with matching value should be rejected.
     * @return `this`.
     */
    fun <T : Any> rejectSpansByAttributeValue(
        attributeKey: AttributeKey<T>,
        attributeValuePredicate: (T) -> Boolean
    ): SpanFilterBuilder {
        rejectSpanAttributes[attributeKey] = attributeValuePredicate as (Any) -> Boolean
        return this
    }

    /**
     * Modify span data before it enters the exporter pipeline.
     *
     *
     * Any attribute with key `attributeKey` and will be removed from the span before it is
     * exported.
     *
     * @param attributeKey An attribute key to match.
     * @return `this`.
     */
    fun <T : Any> removeSpanAttribute(attributeKey: AttributeKey<T>): SpanFilterBuilder = removeSpanAttribute(attributeKey, { true })

    /**
     * Modify span data before it enters the exporter pipeline.
     *
     *
     * Any attribute with key `attributeKey` and value matching the `attributeValuePredicate` will be removed from the span before it is exported.
     *
     * @param attributeKey An attribute key to match.
     * @param attributeValuePredicate A function that returns true if an attribute with matching
     * value should be removed from the span.
     * @return `this`.
     */
    fun <T> removeSpanAttribute(
        attributeKey: AttributeKey<T>,
        attributeValuePredicate: (T) -> Boolean
    ): SpanFilterBuilder = replaceSpanAttribute(
        attributeKey,
        { old -> if (attributeValuePredicate(old)) null else old }
    )

    /**
     * Modify span data before it enters the exporter pipeline.
     *
     *
     * The value of any attribute with key `attributeKey` will be passed to the `attributeValueModifier` function. The value returned by the function will replace the
     * original value. When the modifier function returns `null` the attribute will be removed
     * from the span.
     *
     * @param attributeKey An attribute key to match.
     * @param attributeValueModifier A function that receives the old attribute value and returns
     * the new one.
     * @return `this`.
     */
    fun <T> replaceSpanAttribute(
        attributeKey: AttributeKey<T>,
        attributeValueModifier: (T) -> T?
    ): SpanFilterBuilder {
        spanAttributeReplacements[attributeKey] = attributeValueModifier as (Any) -> Any?
        return this
    }
}

internal fun SpanFilterBuilder.toSpanInterceptor(): ((SpanData) -> SpanData?) {
    val filter: (SpanData) -> SpanData? = filter@{ spanData: SpanData ->
        // Step 1: reject spans by name
        val rejectName = rejectSpanNames.any { predicate ->
            predicate(spanData.name)
        }
        if (rejectName) {
            return@filter null
        }

        // Step 2: reject spans by attribute value
        val rejectAttribute = rejectSpanAttributes.any { predicate ->
            val attribute = spanData.attributes.get(predicate.key)
            if (attribute != null) {
                predicate.value(attribute)
            } else {
                false
            }
        }
        if (rejectAttribute) {
            return@filter null
        }

        if (spanAttributeReplacements.isEmpty()) {
            return@filter spanData
        }

        // Step 3: replace attributes
        val newAttributes = Attributes.builder()
        spanData.attributes.forEach { key, attrValue ->
            val reMapper = spanAttributeReplacements[key]
            if (reMapper != null) {
                val newValue = reMapper.invoke(attrValue)
                newValue?.let {
                    newAttributes.put(key as AttributeKey<Any>, newValue)
                }
            }
        }

        return@filter ModifiedSpanData(original = spanData, modifiedAttributes = newAttributes.build())
    }
    return filter
}
