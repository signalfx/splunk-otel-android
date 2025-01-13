/*
 * Copyright 2024 Splunk Inc.
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

package com.splunk.rum.library.okhttp3;

import static java.util.Collections.singletonList;

import com.splunk.rum.library.common.HttpConfigUtil;
import com.splunk.rum.library.common.HttpInstrumentationConfig;
import com.splunk.rum.library.okhttp3.tracing.ConnectionErrorWrapperInterceptor;
import com.splunk.rum.library.okhttp3.tracing.OkHttpCallbackAdviceHelper;
import com.splunk.rum.library.okhttp3.tracing.OkHttpCustomConfigArgument;
import com.splunk.rum.library.okhttp3.tracing.ResendCountContextInterceptor;
import com.splunk.rum.library.okhttp3.tracing.TracingInterceptor;
import com.cisco.mrum.common.otel.api.OpenTelemetry;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.net.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.ConnectionErrorSpanInterceptor;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpInstrumenterFactory;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class OkHttp3Singletons {

    private static final OpenTelemetrySdk openTelemetrySdkInstance;

    static {
        openTelemetrySdkInstance = OpenTelemetry.INSTANCE.getInstance();
    }

    private static final Instrumenter<Request, Response> INSTRUMENTER =
            OkHttpInstrumenterFactory.create(
                    openTelemetrySdkInstance,
                    builder ->
                            builder.setCapturedRequestHeaders(
                                            HttpInstrumentationConfig.getCapturedRequestHeaders())
                                    .setCapturedResponseHeaders(
                                            HttpInstrumentationConfig
                                                    .getCapturedResponseHeaders())
                                    .setKnownMethods(HttpInstrumentationConfig.getKnownMethods()),
                    spanNameExtractorConfigurer ->
                            spanNameExtractorConfigurer.setKnownMethods(
                                    HttpInstrumentationConfig.getKnownMethods()),
                    singletonList(
                            PeerServiceAttributesExtractor.create(
                                    OkHttpAttributesGetter.INSTANCE,
                                    HttpInstrumentationConfig.newPeerServiceResolver())),
                    HttpInstrumentationConfig.emitExperimentalHttpClientMetrics());

    public static final Interceptor TRACING_INTERCEPTOR =
            new TracingInterceptor(INSTRUMENTER, openTelemetrySdkInstance.getPropagators());

    public static final Interceptor CALLBACK_CONTEXT_INTERCEPTOR =
            chain -> {
                Request originalRequest = chain.request();

                //Retrieve the network tracing enabled flag from module/remote configuration in the first interceptor
                boolean isNetworkTracingEnabled = HttpConfigUtil.isNetworkTracingEnabled();

                //Pass network tracing enabled flag to the interceptor chain via Request tag
                OkHttpCustomConfigArgument okHttpCustomConfigArgument = new OkHttpCustomConfigArgument(isNetworkTracingEnabled);
                Request taggedRequest = originalRequest.newBuilder()
                        .tag(OkHttpCustomConfigArgument.class, okHttpCustomConfigArgument)
                        .build();

                if (isNetworkTracingEnabled) {
                    //Use originalRequest to get the parent context if one exists as originalRequest is the key in
                    //OkHttpCallbackAdviceHelper VirtualField
                    Context context =
                            OkHttpCallbackAdviceHelper.tryRecoverPropagatedContextFromCallback(originalRequest);
                    if (context != null) {
                        try (Scope ignored = context.makeCurrent()) {
                            return chain.proceed(taggedRequest);
                        }
                    }
                }

                return chain.proceed(taggedRequest);
            };

    public static final Interceptor RESEND_COUNT_CONTEXT_INTERCEPTOR =
            new ResendCountContextInterceptor();

    public static final Interceptor CONNECTION_ERROR_INTERCEPTOR =
            new ConnectionErrorWrapperInterceptor(new ConnectionErrorSpanInterceptor(INSTRUMENTER));

    private OkHttp3Singletons() {
    }
}
