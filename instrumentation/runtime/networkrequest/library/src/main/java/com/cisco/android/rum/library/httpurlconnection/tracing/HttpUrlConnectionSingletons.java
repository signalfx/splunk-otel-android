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

package com.cisco.android.rum.library.httpurlconnection.tracing;

import com.cisco.mrum.common.otel.api.OpenTelemetry;
import com.cisco.android.rum.library.common.HttpInstrumentationConfig;

import java.net.URLConnection;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientPeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpExperimentalAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.sdk.OpenTelemetrySdk;

public final class HttpUrlConnectionSingletons {

    private static final Instrumenter<URLConnection, Integer> INSTRUMENTER;
    private static final OpenTelemetrySdk openTelemetrySdkInstance;
    private static final String INSTRUMENTATION_NAME = "cisco.mrum.auto-http-url-connection";

    static {

        openTelemetrySdkInstance = OpenTelemetry.INSTANCE.getInstance();

        HttpUrlHttpAttributesGetter httpAttributesGetter = new HttpUrlHttpAttributesGetter();

        HttpSpanNameExtractorBuilder<URLConnection> httpSpanNameExtractorBuilder =
                HttpSpanNameExtractor.builder(httpAttributesGetter)
                        .setKnownMethods(HttpInstrumentationConfig.getKnownMethods());

        HttpClientAttributesExtractorBuilder<URLConnection, Integer>
                httpClientAttributesExtractorBuilder =
                HttpClientAttributesExtractor.builder(httpAttributesGetter)
                        .setCapturedRequestHeaders(
                                HttpInstrumentationConfig.getCapturedRequestHeaders())
                        .setCapturedResponseHeaders(
                                HttpInstrumentationConfig.getCapturedResponseHeaders())
                        .setKnownMethods(HttpInstrumentationConfig.getKnownMethods());

        HttpClientPeerServiceAttributesExtractor<URLConnection, Integer>
                httpClientPeerServiceAttributesExtractor =
                HttpClientPeerServiceAttributesExtractor.create(
                        httpAttributesGetter,
                        HttpInstrumentationConfig.newPeerServiceResolver());

        //openTelemetrySdkInstance would never be null as it's initialized in application onCreate which is
        //prior to this. This is just an extra cautious check.
        if (openTelemetrySdkInstance != null) {
            InstrumenterBuilder<URLConnection, Integer> builder =
                    Instrumenter.<URLConnection, Integer>builder(
                                    openTelemetrySdkInstance,
                                    INSTRUMENTATION_NAME,
                                    httpSpanNameExtractorBuilder.build())
                            .setSpanStatusExtractor(
                                    HttpSpanStatusExtractor.create(httpAttributesGetter))
                            .addAttributesExtractor(httpClientAttributesExtractorBuilder.build())
                            .addAttributesExtractor(httpClientPeerServiceAttributesExtractor)
                            .addOperationMetrics(HttpClientMetrics.get());

            if (HttpInstrumentationConfig.emitExperimentalHttpClientMetrics()) {
                builder.addAttributesExtractor(
                                HttpExperimentalAttributesExtractor.create(httpAttributesGetter))
                        .addOperationMetrics(HttpClientExperimentalMetrics.get());
            }

            INSTRUMENTER = builder.buildClientInstrumenter(RequestPropertySetter.INSTANCE);
        } else {
            INSTRUMENTER = null;
        }
    }

    public static Instrumenter<URLConnection, Integer> instrumenter() {
        return INSTRUMENTER;
    }

    public static OpenTelemetrySdk openTelemetrySdkInstance() {
        return openTelemetrySdkInstance;
    }

    private HttpUrlConnectionSingletons() {
    }
}

