package com.splunk.rum.integration.httpurlconnection

import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.utils.ServerTimingHeaderParser
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import java.net.URLConnection

class HttpURLAdditionalAttributesExtractor: AttributesExtractor<URLConnection, Int> {

    override fun onStart(attributes: AttributesBuilder, parentContext: Context, connection: URLConnection) {
        attributes.put(RumConstants.COMPONENT_KEY, "http")
    }

    override fun onEnd(
        attributes: AttributesBuilder,
        context: Context,
        connection: URLConnection,
        responseCode: Int?,
        error: Throwable?
    ) {
        onResponse(attributes, connection)
    }

    private fun onResponse(attributes: AttributesBuilder, connection: URLConnection) {
        connection.headerFields.forEach { header ->
            if (!header.key.equals(RumConstants.SERVER_TIMING_HEADER, ignoreCase = true)) {
                return@forEach
            }

            // HttpURLConnection consolidates multiple headers with the same name into a single header entry,
            // combining their values into a list. We want to capture the last valid server-timing header.
            header.value.forEach { headerValue ->
                val ids = ServerTimingHeaderParser.parse(headerValue)
                if (ids.size == 2) {
                    attributes.put(RumConstants.LINK_TRACE_ID_KEY, ids[0])
                    attributes.put(RumConstants.LINK_SPAN_ID_KEY, ids[1])
                }
            }
        }
    }
}