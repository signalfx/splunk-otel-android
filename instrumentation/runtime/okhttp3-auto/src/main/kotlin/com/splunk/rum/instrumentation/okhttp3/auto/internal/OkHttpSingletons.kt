/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.instrumentation.okhttp3.auto.internal

import com.splunk.rum.instrumentation.okhttp3.auto.OkHttpInstrumentation
import com.splunk.rum.instrumentation.okhttp3.common.internal.ConnectionErrorSpanInterceptor
import com.splunk.rum.instrumentation.okhttp3.common.internal.OkHttpAttributesGetter
import com.splunk.rum.instrumentation.okhttp3.common.internal.OkHttpClientInstrumenterBuilderFactory
import com.splunk.rum.instrumentation.okhttp3.common.internal.TracingInterceptor
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceAttributesExtractor
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientRequestResendCount
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
object OkHttpSingletons {
    private val NOOP_INTERCEPTOR = Interceptor { chain -> chain.proceed(chain.request()) }

    @JvmField
    var connectionErrorInterceptor: Interceptor = NOOP_INTERCEPTOR

    @JvmField
    var tracingInterceptor: Interceptor = NOOP_INTERCEPTOR

    fun configure(instrumentation: OkHttpInstrumentation, openTelemetry: OpenTelemetry) {
        var instrumenterBuilder: DefaultHttpClientInstrumenterBuilder<Interceptor.Chain, Response> =
            OkHttpClientInstrumenterBuilderFactory
                .create(openTelemetry)
                .setCapturedRequestHeaders(instrumentation.capturedRequestHeaders)
                .setCapturedResponseHeaders(instrumentation.capturedResponseHeaders)
                .setKnownMethods(instrumentation.knownMethods)
                // TODO: Do we really need to set the known methods on the span name extractor as well?
                .setSpanNameExtractor {
                    HttpSpanNameExtractor.builder(OkHttpAttributesGetter.INSTANCE)
                        .setKnownMethods(instrumentation.knownMethods)
                        .build()
                }
                .addAttributesExtractor(
                    PeerServiceAttributesExtractor.create(
                        OkHttpAttributesGetter.INSTANCE,
                        instrumentation.newPeerServiceResolver()
                    )
                )
                .setEmitExperimentalHttpClientTelemetry(
                    instrumentation.emitExperimentalHttpClientTelemetry()
                )

        for (extractor in instrumentation.additionalExtractors) {
            instrumenterBuilder = instrumenterBuilder.addAttributesExtractor(extractor)
        }

        val instrumenter: Instrumenter<Interceptor.Chain, Response> = instrumenterBuilder.build()

        connectionErrorInterceptor = ConnectionErrorSpanInterceptor(instrumenter)
        tracingInterceptor = TracingInterceptor(instrumenter, openTelemetry.propagators)
    }

    @JvmField
    val callbackContextInterceptor: Interceptor =
        Interceptor { chain ->
            val request: Request = chain.request()
            val context: Context? =
                OkHttpCallbackAdviceHelper.tryRecoverPropagatedContextFromCallback(request)
            if (context != null) {
                val ignored: Scope = context.makeCurrent()
                ignored.use {
                    return@Interceptor chain.proceed(request)
                }
            }
            chain.proceed(request)
        }

    @JvmField
    val resendCountContextInterceptor: Interceptor =
        Interceptor { chain ->
            val ignored: Scope = HttpClientRequestResendCount.initialize(Context.current()).makeCurrent()
            ignored.use {
                chain.proceed(chain.request())
            }
        }
}
