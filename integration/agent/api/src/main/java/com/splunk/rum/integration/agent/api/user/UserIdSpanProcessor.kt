package com.splunk.rum.integration.agent.api.user

import com.splunk.rum.integration.agent.api.attributes.AttributeConstants.USER_ID_KEY
import com.splunk.rum.integration.agent.internal.user.IUserManager
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

internal class UserIdSpanProcessor(private val userManager: IUserManager) : SpanProcessor {
    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        val userId = userManager.userId ?: return
        span.setAttribute(USER_ID_KEY, userId)
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) = Unit

    override fun isEndRequired(): Boolean = false
}