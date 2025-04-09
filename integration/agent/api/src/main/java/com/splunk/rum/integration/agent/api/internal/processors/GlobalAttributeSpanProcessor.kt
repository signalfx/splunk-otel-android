package com.splunk.rum.integration.agent.api.internal.processors

import com.splunk.rum.integration.agent.api.attributes.MutableAttributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class GlobalAttributeSpanProcessor(private val globalAttributes: MutableAttributes) : SpanProcessor {

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        span.setAllAttributes(globalAttributes.getAll())
    }

    override fun isStartRequired(): Boolean {
        return true
    }

    override fun onEnd(span: ReadableSpan) {
    }

    override fun isEndRequired(): Boolean {
        return false
    }
}