package com.splunk.rum.integration.agent.api.attributes

import android.app.Application
import com.splunk.rum.integration.agent.internal.session.SessionManager
import com.splunk.sdk.utils.ErrorIdentifierExtractor
import com.splunk.sdk.utils.ErrorIdentifierInfo
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

internal class ErrorIdentifierAttributesSpanProcessor(private val application: Application) : SpanProcessor {
    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        val extractor = ErrorIdentifierExtractor(application)
        val errorIdentifierInfo: ErrorIdentifierInfo = extractor.extractInfo()
        if (errorIdentifierInfo.applicationId != null) {
            span.setAttribute("service.application_id", errorIdentifierInfo.applicationId)
        }
        // etc

    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) = Unit

    override fun isEndRequired(): Boolean = false
}
