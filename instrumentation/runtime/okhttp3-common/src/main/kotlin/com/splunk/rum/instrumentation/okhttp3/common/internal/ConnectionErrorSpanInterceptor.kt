/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.rum.instrumentation.okhttp3.common.internal

import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientRequestResendCount
import java.io.IOException
import java.time.Instant
import okhttp3.Interceptor
import okhttp3.Response

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class ConnectionErrorSpanInterceptor(private val instrumenter: Instrumenter<Interceptor.Chain, Response>) :
    Interceptor {

    @Suppress("NewApi") // Requires API 26 or core library desugaring in the host app.
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val parentContext = Context.current()
        var response: Response? = null
        var error: Throwable? = null
        val startTime = Instant.now()
        try {
            response = chain.proceed(request)
            return response
        } catch (t: Throwable) {
            error = t
            throw t
        } finally {
            // only create a span when there wasn't any HTTP request
            if (HttpClientRequestResendCount.get(parentContext) == 0) {
                if (instrumenter.shouldStart(parentContext, chain)) {
                    InstrumenterUtil.startAndEnd(
                        instrumenter,
                        parentContext,
                        chain,
                        response,
                        error,
                        startTime,
                        Instant.now()
                    )
                }
            }
        }
    }
}
