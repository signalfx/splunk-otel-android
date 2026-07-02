/*
 * Copyright The OpenTelemetry Authors
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
