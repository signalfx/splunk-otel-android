package com.splunk.rum.integration.agent.api.attributes

import android.app.Application
import android.util.Log
import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.utils.ErrorIdentifierExtractor
import com.splunk.sdk.utils.ErrorIdentifierInfo
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor

internal class ErrorIdentifierAttributesSpanProcessor(private val application: Application) : SpanProcessor {

    private var applicationId: String? = null
    private var versionCode: String? = null
    private var customUUID: String? = null
    private var isInitialized = false

    private fun initializeAttributes() {
        if (!isInitialized) {
            val extractor = ErrorIdentifierExtractor(application)
            val errorIdentifierInfo: ErrorIdentifierInfo = extractor.extractInfo()

            applicationId = errorIdentifierInfo.applicationId
            versionCode = errorIdentifierInfo.versionCode
            customUUID = errorIdentifierInfo.customUUID

            isInitialized = true
        }
    }

    init {
        initializeAttributes()
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
