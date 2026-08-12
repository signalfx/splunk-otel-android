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

package com.splunk.rum.instrumentation.httpurlconnection.auto.internal

import com.splunk.rum.instrumentation.httpurlconnection.auto.HttpUrlInstrumentation
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientExperimentalMetrics
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientPeerServiceAttributesExtractor
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpExperimentalAttributesExtractor
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor
import java.net.URLConnection

internal object HttpUrlConnectionSingletons {
    private const val INSTRUMENTATION_NAME = "com.splunk.rum.http-url-connection"

    private var instrumenter: Instrumenter<URLConnection, Int>? = null
    private var openTelemetryInstance: OpenTelemetry? = null

    fun configure(instrumentation: HttpUrlInstrumentation, openTelemetry: OpenTelemetry) {
        val httpAttributesGetter = HttpUrlHttpAttributesGetter

        val httpSpanNameExtractor =
            HttpSpanNameExtractor.builder(httpAttributesGetter)
                .setKnownMethods(instrumentation.knownMethods)
                .build()

        val httpClientAttributesExtractor =
            HttpClientAttributesExtractor.builder(httpAttributesGetter)
                .setCapturedRequestHeaders(instrumentation.capturedRequestHeaders)
                .setCapturedResponseHeaders(instrumentation.capturedResponseHeaders)
                .setKnownMethods(instrumentation.knownMethods)
                .build()

        val httpClientPeerServiceAttributesExtractor =
            HttpClientPeerServiceAttributesExtractor.create(
                httpAttributesGetter,
                instrumentation.newPeerServiceResolver()
            )

        openTelemetryInstance = openTelemetry

        val builder =
            Instrumenter.builder<URLConnection, Int>(
                openTelemetry,
                INSTRUMENTATION_NAME,
                httpSpanNameExtractor
            )
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributesGetter))
                .addAttributesExtractor(httpClientAttributesExtractor)
                .addAttributesExtractor(httpClientPeerServiceAttributesExtractor)
                .addOperationMetrics(HttpClientMetrics.get())

        for (extractor in instrumentation.additionalExtractors) {
            builder.addAttributesExtractor(extractor)
        }

        if (instrumentation.emitExperimentalHttpClientMetrics()) {
            builder
                .addAttributesExtractor(HttpExperimentalAttributesExtractor.create(httpAttributesGetter))
                .addOperationMetrics(HttpClientExperimentalMetrics.get())
        }

        instrumenter = builder.buildClientInstrumenter(RequestPropertySetter)
    }

    fun instrumenter(): Instrumenter<URLConnection, Int>? = instrumenter

    fun openTelemetryInstance(): OpenTelemetry? = openTelemetryInstance
}
