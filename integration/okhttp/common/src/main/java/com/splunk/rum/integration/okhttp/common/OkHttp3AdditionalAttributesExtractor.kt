package com.splunk.rum.integration.okhttp.common

import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.utils.ServerTimingHeaderParser
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import okhttp3.Interceptor
import okhttp3.Response

class OkHttp3AdditionalAttributesExtractor : AttributesExtractor<Interceptor.Chain, Response> {

    override fun onStart(attributes: AttributesBuilder, parentContext: Context, request: Interceptor.Chain) {
        attributes.put(RumConstants.COMPONENT_KEY, RumConstants.COMPONENT_HTTP)
    }

    override fun onEnd(
        attributes: AttributesBuilder,
        context: Context,
        chain: Interceptor.Chain,
        response: Response?,
        error: Throwable?
    ) {
        onResponse(attributes, response)
    }

    private fun onResponse(attributes: AttributesBuilder, response: Response?) {
        response?.headers?.forEach { header ->
            if (!header.first.equals(RumConstants.SERVER_TIMING_HEADER, ignoreCase = true)) {
                return@forEach
            }

            val serverTraceContext = ServerTimingHeaderParser.parse(header.second)
            serverTraceContext?.let {
                attributes.put(RumConstants.LINK_TRACE_ID_KEY, it.traceId)
                attributes.put(RumConstants.LINK_SPAN_ID_KEY, it.spanId)
            }
        }
    }
}
