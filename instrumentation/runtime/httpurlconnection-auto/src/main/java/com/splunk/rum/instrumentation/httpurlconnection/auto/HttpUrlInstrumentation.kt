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

package com.splunk.rum.instrumentation.httpurlconnection.auto

import com.splunk.rum.instrumentation.httpurlconnection.auto.internal.HttpUrlConnectionSingletons
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.internal.HttpConstants
import java.net.URLConnection

/** Runtime configuration for HttpURLConnection auto-instrumentation. */
class HttpUrlInstrumentation {
    val additionalExtractors: MutableList<AttributesExtractor<URLConnection, Int>> = mutableListOf()

    var capturedRequestHeaders: List<String> = emptyList()
        set(requestHeaders) {
            field = requestHeaders.toMutableList()
        }

    var capturedResponseHeaders: List<String> = emptyList()
        set(responseHeaders) {
            field = responseHeaders.toMutableList()
        }

    var knownMethods: Set<String> = HttpConstants.KNOWN_METHODS
        set(knownMethods) {
            field = knownMethods.toMutableSet()
        }

    private var peerServiceMapping: Map<String, String> = emptyMap()
    private var emitExperimentalHttpClientMetrics = false

    // Time (ms) to wait before assuming that an idle connection is no longer in use and should be reported.
    private var connectionInactivityTimeoutMs: Long = DEFAULT_CONNECTION_INACTIVITY_TIMEOUT_MS

    /** Adds an [AttributesExtractor] that will extract additional attributes. */
    fun addAttributesExtractor(extractor: AttributesExtractor<URLConnection, Int>) {
        additionalExtractors.add(extractor)
    }

    /** Configures the extractor of the `peer.service` span attribute. */
    fun setPeerServiceMapping(peerServiceMapping: Map<String, String>) {
        this.peerServiceMapping = peerServiceMapping.toMap()
    }

    fun newPeerServiceResolver(): PeerServiceResolver = PeerServiceResolver.create(peerServiceMapping)

    /** When enabled, emits experimental HTTP client metrics. */
    fun setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics: Boolean) {
        this.emitExperimentalHttpClientMetrics = emitExperimentalHttpClientMetrics
    }

    fun emitExperimentalHttpClientMetrics(): Boolean = emitExperimentalHttpClientMetrics

    fun install(openTelemetry: OpenTelemetry) {
        HttpUrlConnectionSingletons.configure(this, openTelemetry)
    }

    /**
     * Configures the connection inactivity timeout in milliseconds used by the idle connection
     * harvester to find idle connections that should be reported.
     *
     * @param timeoutMs the timeout period in milliseconds. Must be non-negative.
     * @throws IllegalArgumentException if [timeoutMs] is negative.
     */
    fun setConnectionInactivityTimeoutMs(timeoutMs: Long) {
        require(timeoutMs >= 0) { "timeoutMs must be non-negative" }
        connectionInactivityTimeoutMs = timeoutMs
    }

    /**
     * Configures the connection inactivity timeout in milliseconds for testing purposes only.
     */
    internal fun setConnectionInactivityTimeoutMsForTesting(timeoutMsForTesting: Long) {
        connectionInactivityTimeoutMs = timeoutMsForTesting
    }

    /**
     * Returns a runnable that can be scheduled to run periodically at a fixed interval to close
     * open spans if a connection is left idle for [connectionInactivityTimeoutMs].
     */
    fun getReportIdleConnectionRunnable(): Runnable = Runnable {
        HttpUrlReplacements.reportIdleConnectionsOlderThan(connectionInactivityTimeoutMs)
    }

    /**
     * The interval duration in milliseconds that the runnable from [getReportIdleConnectionRunnable]
     * should be scheduled to periodically run at.
     */
    fun getReportIdleConnectionInterval(): Long = connectionInactivityTimeoutMs

    private companion object {
        private const val DEFAULT_CONNECTION_INACTIVITY_TIMEOUT_MS = 10_000L
    }
}
