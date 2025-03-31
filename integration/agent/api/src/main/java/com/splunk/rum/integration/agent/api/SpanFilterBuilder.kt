package com.splunk.rum.integration.agent.api


import io.opentelemetry.android.export.SpanDataModifier
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.function.Function
import java.util.function.Predicate

/** Delegating wrapper around otel SpanDataModifier.  */
class SpanFilterBuilder internal constructor(exporter: SpanExporter) {
    private val spanDataModifier: SpanDataModifier = SpanDataModifier.builder(exporter)

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
        // TODO: Use pure upstream mechanism for this
        spanDataModifier.rejectSpansByName(spanNamePredicate)
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
    fun <T> rejectSpansByAttributeValue(
        attributeKey: AttributeKey<T>,
        attributeValuePredicate: (T) -> Boolean,
    ): SpanFilterBuilder {
        spanDataModifier.rejectSpansByAttributeValue(attributeKey, attributeValuePredicate)
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
    fun <T> removeSpanAttribute(attributeKey: AttributeKey<T>): SpanFilterBuilder {
        return removeSpanAttribute(attributeKey, { true })
    }

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
    ): SpanFilterBuilder {
        return replaceSpanAttribute(
            attributeKey,
            { old: T -> if (attributeValuePredicate.invoke(old)) null else old }
        )
    }

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
        spanDataModifier.replaceSpanAttribute(attributeKey, attributeValueModifier)
        return this
    }

    internal fun build(): SpanExporter {
        return spanDataModifier.build()
    }
}