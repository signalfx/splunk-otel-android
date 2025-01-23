/*
 * Copyright Splunk Inc.
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

package com.splunk.rum;

import com.android.volley.toolbox.HttpResponse;
import com.android.volley.toolbox.HurlStack;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import javax.net.ssl.SSLSocketFactory;

/**
 * Entrypoint for tracing Volley clients.
 *
 * @deprecated This API is deprecated and will be removed in a future release.
 */
@Deprecated
public final class VolleyTracing {

    /**
     * Returns a new {@link VolleyTracing} configured with the given {@link SplunkRum}.
     *
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    public static VolleyTracing create(SplunkRum splunkRum) {
        return create(splunkRum.getOpenTelemetry());
    }

    /**
     * Returns a new {@link VolleyTracing} configured with the given {@link OpenTelemetry}.
     *
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    public static VolleyTracing create(OpenTelemetry openTelemetry) {
        return builder(openTelemetry).build();
    }

    /**
     * Returns a new {@link VolleyTracingBuilder} configured with the given {@link SplunkRum}.
     *
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    public static VolleyTracingBuilder builder(SplunkRum splunkRum) {
        return new VolleyTracingBuilder(splunkRum.getOpenTelemetry());
    }

    /**
     * Returns a new {@link VolleyTracingBuilder} configured with the given {@link OpenTelemetry}.
     *
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    public static VolleyTracingBuilder builder(OpenTelemetry openTelemetry) {
        return new VolleyTracingBuilder(openTelemetry);
    }

    private final Instrumenter<RequestWrapper, HttpResponse> instrumenter;

    /**
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    VolleyTracing(Instrumenter<RequestWrapper, HttpResponse> instrumenter) {
        this.instrumenter = instrumenter;
    }

    /**
     * Returns a new {@link HurlStack} capable of tracing requests.
     *
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    public HurlStack newHurlStack() {
        return new TracingHurlStack(instrumenter);
    }

    /**
     * Returns a new {@link HurlStack} capable of tracing requests configured with given {@link
     * HurlStack.UrlRewriter}.
     *
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    public HurlStack newHurlStack(HurlStack.UrlRewriter urlRewriter) {
        return new TracingHurlStack(instrumenter, urlRewriter);
    }

    /**
     * Returns a new {@link HurlStack} capable of tracing requests configured with given {@link
     * HurlStack.UrlRewriter} and {@link SSLSocketFactory}.
     *
     * @deprecated This method is deprecated and will be removed in a future release.
     */
    @Deprecated
    public HurlStack newHurlStack(
            HurlStack.UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        return new TracingHurlStack(instrumenter, urlRewriter, sslSocketFactory);
    }
}
