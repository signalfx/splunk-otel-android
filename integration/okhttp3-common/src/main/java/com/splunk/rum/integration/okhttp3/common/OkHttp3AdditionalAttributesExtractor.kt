/*
 * Copyright 2025 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum.integration.okhttp3.common

import com.splunk.rum.common.otel.internal.RumConstants
import com.splunk.rum.utils.ServerTimingHeaderParser
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import okhttp3.Interceptor
import okhttp3.Response

class OkHttp3AdditionalAttributesExtractor : AttributesExtractor<Interceptor.Chain, Response> {

    override fun onStart(attributes: AttributesBuilder, parentContext: Context, chain: Interceptor.Chain) {
        attributes.put(RumConstants.COMPONENT_KEY, RumConstants.COMPONENT_HTTP)
    }

    override fun onEnd(
        attributes: AttributesBuilder,
        context: Context,
        chain: Interceptor.Chain,
        response: Response?,
        error: Throwable?
    ) {
        addPayloadAttributes(attributes, chain, response)
        addServerContext(attributes, response)
    }

    private fun addPayloadAttributes(attributes: AttributesBuilder, chain: Interceptor.Chain, response: Response?) {
        val requestBodySize: Long? = chain.request().header("content-length")?.toLongOrNull()?.sanitizeUnknown()
        attributes.put(RumConstants.HTTP_REQUEST_BODY_SIZE, requestBodySize)

        if (response != null) {
            val responseBodySize: Long? = response.header("content-length")?.toLongOrNull()?.sanitizeUnknown()
            attributes.put(RumConstants.HTTP_RESPONSE_BODY_SIZE, responseBodySize)
        }
    }

    // HTTP spec uses -1 to indicate unknown length. Return null instead of -1.
    private fun Long?.sanitizeUnknown(): Long? = if (this != null && this >= 0) this else null

    private fun addServerContext(attributes: AttributesBuilder, response: Response?) {
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
