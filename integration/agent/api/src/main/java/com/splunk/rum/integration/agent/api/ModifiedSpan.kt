package com.splunk.rum.integration.agent.api

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.SpanData

internal class ModifiedSpanData(
    original: SpanData,
    private val modifiedAttributes: Attributes
) : DelegatingSpanData(original) {
    override fun getAttributes(): Attributes {
        return modifiedAttributes
    }

    override fun getTotalAttributeCount(): Int {
        return modifiedAttributes.size()
    }
}