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
