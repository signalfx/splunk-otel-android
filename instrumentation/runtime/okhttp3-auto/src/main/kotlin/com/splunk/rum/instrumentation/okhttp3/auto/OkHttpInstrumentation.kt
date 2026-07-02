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

package com.splunk.rum.instrumentation.okhttp3.auto

import com.splunk.rum.instrumentation.okhttp3.auto.internal.OkHttpSingletons
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.internal.HttpConstants
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Runtime configuration for OkHttp3 auto-instrumentation.
 */
class OkHttpInstrumentation {
    val additionalExtractors: MutableList<AttributesExtractor<Interceptor.Chain, Response>> =
        mutableListOf()

    /**
     * Configures the HTTP request headers that will be captured as span attributes.
     */
    var capturedRequestHeaders: List<String> = listOf()
        set(requestHeaders) {
            field = requestHeaders.toMutableList()
        }

    /**
     * Configures the HTTP response headers that will be captured as span attributes.
     */
    var capturedResponseHeaders: List<String> = listOf()
        set(responseHeaders) {
            field = responseHeaders.toMutableList()
        }

    /**
     * Configures the attrs extractor to recognize an alternative set of HTTP request methods.
     *
     * Note: calling this method **overrides** the default known method sets completely.
     */
    var knownMethods: Set<String> = HttpConstants.KNOWN_METHODS
        set(knownMethods) {
            field = knownMethods.toMutableSet()
        }

    private var emitExperimentalHttpClientTelemetry = false
    private var peerServiceMapping: Map<String, String> = emptyMap()

    /** Adds an [AttributesExtractor] that will extract additional attributes. */
    fun addAttributesExtractor(extractor: AttributesExtractor<Interceptor.Chain, Response>) {
        additionalExtractors.add(extractor)
    }

    /** Configures whether experimental HTTP client telemetry should be emitted. */
    fun setEmitExperimentalHttpClientTelemetry(emitExperimentalHttpClientTelemetry: Boolean) {
        this.emitExperimentalHttpClientTelemetry = emitExperimentalHttpClientTelemetry
    }

    /** Configures the extractor of the `peer.service` span attribute. */
    fun setPeerServiceMapping(peerServiceMapping: Map<String, String>) {
        this.peerServiceMapping = peerServiceMapping.toMap()
    }

    fun emitExperimentalHttpClientTelemetry(): Boolean = emitExperimentalHttpClientTelemetry

    fun newPeerServiceResolver(): PeerServiceResolver = PeerServiceResolver.create(peerServiceMapping)

    fun install(openTelemetry: OpenTelemetry) {
        OkHttpSingletons.configure(this, openTelemetry)
    }
}
