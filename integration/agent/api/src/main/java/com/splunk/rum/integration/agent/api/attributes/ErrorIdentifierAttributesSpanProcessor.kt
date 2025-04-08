package com.splunk.rum.integration.agent.api.attributes

import android.app.Application
import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.utils.ApplicationInfoUtils
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

internal class ErrorIdentifierAttributesSpanProcessor(application: Application) : SpanProcessor {

    private var applicationId: String? = null
    private var versionCode: String? = null
    private var splunkBuildId: String? = null

    init {
        applicationId = ApplicationInfoUtils.retrieveApplicationId(application)
        versionCode = ApplicationInfoUtils.retrieveVersionCode(application)
        splunkBuildId = ApplicationInfoUtils.retrieveSplunkBuildID(application)
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
            splunkBuildId?.let {
                span.setAttribute(RumConstants.SPLUNK_BUILD_ID, it)
            }
        }
    }

    override fun isStartRequired(): Boolean = true

    override fun onEnd(span: ReadableSpan) = Unit

    override fun isEndRequired(): Boolean = false
}
