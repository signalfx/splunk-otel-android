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

package com.cisco.android.rum.library.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.instrumentation.api.instrumenter.net.PeerServiceResolver;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;

/**
 * Configuration for automatic instrumentation of network requests.
 */
public final class HttpInstrumentationConfig {
    private static List<String> capturedRequestHeaders = new ArrayList<>();
    private static List<String> capturedResponseHeaders = new ArrayList<>();
    private static Set<String> knownMethods = HttpConstants.KNOWN_METHODS;
    private static Map<String, String> peerServiceMapping = new HashMap<>();
    private static boolean emitExperimentalHttpClientMetrics;

    private HttpInstrumentationConfig() {
    }

    /**
     * Configures the HTTP request headers that will be captured as span attributes as described in
     * <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/trace/semantic_conventions/http.md#http-request-and-response-headers"> HTTP
     * semantic conventions</a>.
     *
     * <p>The HTTP request header values will be captured under the {@code
     * http.request.header.<name>} attribute key. The {@code <name>} part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param requestHeaders A list of HTTP header names.
     */
    public static void setCapturedRequestHeaders(List<String> requestHeaders) {
        HttpInstrumentationConfig.capturedRequestHeaders = new ArrayList<>(requestHeaders);
    }

    public static List<String> getCapturedRequestHeaders() {
        return capturedRequestHeaders;
    }

    /**
     * Configures the HTTP response headers that will be captured as span attributes as described in
     * <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/trace/semantic_conventions/http.md#http-request-and-response-headers"> HTTP
     * semantic conventions</a>.
     *
     * <p>The HTTP response header values will be captured under the {@code
     * http.response.header.<name>} attribute key. The {@code <name>} part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param responseHeaders A list of HTTP header names.
     */
    public static void setCapturedResponseHeaders(List<String> responseHeaders) {
        HttpInstrumentationConfig.capturedResponseHeaders = new ArrayList<>(responseHeaders);
    }

    public static List<String> getCapturedResponseHeaders() {
        return capturedResponseHeaders;
    }

    /**
     * Configures the attrs extractor to recognize an alternative set of HTTP request methods.
     *
     * <p>By default, the extractor defines "known" methods as the ones listed in <a
     * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-methods">RFC9110</a> and the PATCH
     * method defined in <a href="https://www.rfc-editor.org/rfc/rfc5789.html">RFC5789</a>. If an
     * unknown method is encountered, the extractor will use the value {@value HttpConstants#_OTHER}
     * instead of it and put the original value in an extra {@code http.request.method_original}
     * attribute.
     *
     * <p>Note: calling this method <b>overrides</b> the default known method sets completely; it
     * does not supplement it.
     *
     * @param knownMethods A set of recognized HTTP request methods.
     */
    public static void setKnownMethods(Set<String> knownMethods) {
        HttpInstrumentationConfig.knownMethods = new HashSet<>(knownMethods);
    }

    public static Set<String> getKnownMethods() {
        return knownMethods;
    }

    /**
     * Configures the extractor of the {@code peer.service} span attribute, described in <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/trace/semantic_conventions/span-general.md#general-remote-service-attributes">the
     * specification</a>.
     */
    public static void setPeerServiceMapping(Map<String, String> peerServiceMapping) {
        HttpInstrumentationConfig.peerServiceMapping = new HashMap<>(peerServiceMapping);
    }

    public static PeerServiceResolver newPeerServiceResolver() {
        return PeerServiceResolver.create(peerServiceMapping);
    }

    /**
     * When enabled keeps track of <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/v1.20.0/specification/metrics/semantic_conventions/http-metrics.md#http-client"> experimental
     * HTTP client metrics</a>.
     */
    public static void setEmitExperimentalHttpClientMetrics(
            boolean emitExperimentalHttpClientMetrics) {
        HttpInstrumentationConfig.emitExperimentalHttpClientMetrics = emitExperimentalHttpClientMetrics;
    }

    public static boolean emitExperimentalHttpClientMetrics() {
        return emitExperimentalHttpClientMetrics;
    }

}

