package com.splunk.rum.integration.agent.api.internal.processors

import com.cisco.android.common.utils.extensions.forEachFast
import com.splunk.rum.integration.agent.api.attributes.GlobalAttributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

class GlobalAttributeSpanProcessor : SpanProcessor {

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        span.setAllAttributes(GlobalAttributes.instance.getAll())
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