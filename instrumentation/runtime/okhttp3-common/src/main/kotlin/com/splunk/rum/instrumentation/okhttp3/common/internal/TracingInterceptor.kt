/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.rum.instrumentation.okhttp3.common.internal

import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class TracingInterceptor(
    private val instrumenter: Instrumenter<Interceptor.Chain, Response>,
    private val propagators: ContextPropagators
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val parentContext = Context.current()

        if (!instrumenter.shouldStart(parentContext, chain)) {
            return chain.proceed(chain.request())
        }

        val context = instrumenter.start(parentContext, chain)
        request = injectContextToRequest(request, context)

        val response = try {
            context.makeCurrent().use {
                chain.proceed(request)
            }
        } catch (t: Throwable) {
            instrumenter.end(context, chain, null, t)
            throw t
        }
        instrumenter.end(context, chain, response, null)
        return response
    }

    // Context injection is handled manually because OkHttp Request is immutable.
    private fun injectContextToRequest(request: Request, context: Context): Request {
        val requestBuilder = request.newBuilder()
        propagators
            .getTextMapPropagator()
            .inject(context, requestBuilder, RequestHeaderSetter.INSTANCE)
        return requestBuilder.build()
    }
}
