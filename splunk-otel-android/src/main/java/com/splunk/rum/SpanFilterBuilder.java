/*
 * Copyright Splunk Inc.
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

package com.splunk.rum;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Allows to modify span data before it is sent to the exported. Spans can be modified or entirely
 * rejected from export.
 */
public final class SpanFilterBuilder {

    private Predicate<String> rejectSpanNamesPredicate = spanName -> false;
    private final Map<AttributeKey<String>, Predicate<String>> rejectSpanAttributesPredicates = new HashMap<>();
    private final Map<AttributeKey<String>, Function<String, String>> spanAttributeReplacements = new HashMap<>();

    SpanFilterBuilder() {
    }

    /**
     * Remove matching spans from the exporter pipeline.
     * <p>
     * Span names that match the {@code spanNamePredicate} will not be exported.
     *
     * @param spanNamePredicate A function that returns true if a span with passed name should be
     *                          rejected.
     * @return {@code this}.
     */
    public SpanFilterBuilder rejectSpansByName(Predicate<String> spanNamePredicate) {
        rejectSpanNamesPredicate = rejectSpanNamesPredicate.or(spanNamePredicate);
        return this;
    }

    /**
     * Remove matching spans from the exporter pipeline.
     * <p>
     * Span that contains an attribute with key {@code attributeKey} and value matching the
     * {@code attributeValuePredicate} will not be exported.
     *
     * @param attributeKey            An attribute key to match.
     * @param attributeValuePredicate A function that returns true if a span containing an attribute
     *                                with matching value should be rejected.
     * @return {@code this}.
     */
    public SpanFilterBuilder rejectSpansByAttributeValue(
            AttributeKey<String> attributeKey,
            Predicate<String> attributeValuePredicate) {

        rejectSpanAttributesPredicates.compute(attributeKey,
                (k, oldValue) -> oldValue == null
                        ? attributeValuePredicate
                        : oldValue.or(attributeValuePredicate));
        return this;
    }

    public SpanFilterBuilder removeSpanAttribute(AttributeKey<String> attributeKey) {
        return removeSpanAttribute(attributeKey, value -> true);
    }

    /**
     * Modify span data before it enters the exporter pipeline.
     * <p>
     * An attribute with key {@code attributeKey} and value matching the
     * {@code attributeValuePredicate} will be removed from the span before it is exported.
     *
     * @param attributeKey            An attribute key to match.
     * @param attributeValuePredicate A function that returns true if an attribute with matching
     *                                value should be removed from the span.
     * @return {@code this}.
     */
    public SpanFilterBuilder removeSpanAttribute(
            AttributeKey<String> attributeKey,
            Predicate<String> attributeValuePredicate) {

        return replaceSpanAttribute(attributeKey, old -> attributeValuePredicate.test(old) ? null : old);
    }

    /**
     * Modify span data before it enters the exporter pipeline.
     * <p>
     * Value of an attribute with key {@code attributeKey} will be passed to the
     * {@code attributeValueModifier} function. Value returned by the function will replace the
     * original value. When the modifier function returns {@code null} the attribute will be removed
     * from the span.
     *
     * @param attributeKey           An attribute key to match.
     * @param attributeValueModifier A function that receives the old attribute value and returns
     *                               the new one.
     * @return {@code this}.
     */
    public SpanFilterBuilder replaceSpanAttribute(
            AttributeKey<String> attributeKey,
            Function<String, String> attributeValueModifier) {

        spanAttributeReplacements.compute(attributeKey,
                (k, oldValue) -> oldValue == null
                        ? attributeValueModifier
                        : oldValue.andThen(attributeValueModifier));
        return this;
    }

    Function<SpanExporter, SpanExporter> build() {
        // make a copy so that the references from the builder are not included in the returned function
        Predicate<String> rejectSpanNamesPredicate = this.rejectSpanNamesPredicate;
        Map<AttributeKey<String>, Predicate<String>> rejectSpanAttributesPredicates = new HashMap<>(this.rejectSpanAttributesPredicates);
        Map<AttributeKey<String>, Function<String, String>> spanAttributeReplacements = new HashMap<>(this.spanAttributeReplacements);

        return exporter -> new SpanFilter(exporter, rejectSpanNamesPredicate, rejectSpanAttributesPredicates, spanAttributeReplacements);
    }
}
