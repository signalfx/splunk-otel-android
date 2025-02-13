package com.splunk.rum.integration.agent.api.attributes

import android.app.Application
import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.utils.ErrorIdentifierExtractor
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

internal class ErrorIdentifierAttributesSpanProcessor(application: Application) : SpanProcessor {

    private var applicationId: String? = null
    private var versionCode: String? = null
    private var customUUID: String? = null

    init {
        val extractor = ErrorIdentifierExtractor(application)

        applicationId = extractor.retrieveApplicationId()
        versionCode = extractor.retrieveVersionCode()
        customUUID = extractor.retrieveCustomUUID()
    }

    override fun onStart(parentContext: Context, span: ReadWriteSpan) {
        if (span.getAttribute(RumConstants.COMPONENT_KEY) == RumConstants.COMPONENT_ERROR ||
            span.getAttribute(RumConstants.COMPONENT_KEY) == RumConstants.COMPONENT_CRASH) {
            applicationId?.let {
                span.setAttribute(RumConstants.APPLICATION_ID_KEY, it)
            }
            versionCode?.let {
                span.setAttribute(RumConstants.APP_VERSION_CODE_KEY, it)
            }
            customUUID?.let {
                span.setAttribute(RumConstants.SPLUNK_OLLY_UUID_KEY, it)
            }
        }
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) = Unit

    override fun isEndRequired(): Boolean = false
}
