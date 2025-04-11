package com.splunk.rum.integration.agent.api.internal.processors

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class GlobalAttributeSpanProcessor(private val globalAttributes: Attributes) : SpanProcessor {

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        span.setAllAttributes(globalAttributes)
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