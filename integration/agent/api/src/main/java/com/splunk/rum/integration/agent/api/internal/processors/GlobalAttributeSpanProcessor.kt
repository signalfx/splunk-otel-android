package com.splunk.rum.integration.agent.api.internal.processors

import com.splunk.rum.integration.agent.api.attributes.GlobalAttributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class GlobalAttributeSpanProcessor : SpanProcessor {

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        // Fetch the current attributes from GlobalAttributes and apply them to the span
        GlobalAttributes.instance.attributes.forEach { attribute ->
            when (attribute) {
                is GlobalAttributes.Attribute.Boolean -> span.setAttribute(attribute.name, attribute.value)
                is GlobalAttributes.Attribute.Double -> span.setAttribute(attribute.name, attribute.value)
                is GlobalAttributes.Attribute.Long -> span.setAttribute(attribute.name, attribute.value)
                is GlobalAttributes.Attribute.String -> span.setAttribute(attribute.name, attribute.value)
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
}