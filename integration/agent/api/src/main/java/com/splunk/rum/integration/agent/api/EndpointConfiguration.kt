/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.agent.api

import com.splunk.android.common.logger.Logger
import java.net.URL

class EndpointConfiguration {

    var traceEndpoint: URL
        private set
    var sessionReplayEndpoint: URL? = null
        private set
    var realm: String? = null
        private set
    var rumAccessToken: String? = null
        private set

    /**
     * @param realm Sets the realm for the beacon to send RUM telemetry to, e.g. "us0", "eu0".
     * @param rumAccessToken Sets the RUM auth token to be used by the RUM library.
     */
    constructor(realm: String, rumAccessToken: String) {
        this.realm = realm
        this.rumAccessToken = rumAccessToken
        this.traceEndpoint = URL("https://rum-ingest.$realm.signalfx.com/v1/traces")
        this.sessionReplayEndpoint = URL("https://rum-ingest.$realm.signalfx.com/v1/logs")
    }

    /**
     * @param trace Sets the "beacon" endpoint URL to be used by the RUM library.
     * If the URL contains an 'auth' query parameter, it will be extracted and removed from the URL.
     * @throws IllegalArgumentException if no auth token is found in the URL
     */
    constructor(trace: URL) {
        this.rumAccessToken = extractAuthToken(trace)
            ?: throw IllegalArgumentException(
                "No auth token found in trace URL. Either:\n" +
                    "  1. Use EndpointConfiguration(realm, rumAccessToken) constructor, or\n" +
                    "  2. Include auth parameter in URL: ?auth=YOUR_TOKEN"
            )
        this.traceEndpoint = removeAuthFromUrl(trace)
    }

    /**
     * @param trace Sets the "beacon" endpoint URL to be used by the RUM library.
     * @param sessionReplay Sets the "session replay" endpoint URL to be used by the RUM library.
     * If either URL contains an 'auth' query parameter, it will be extracted and removed from the URLs.
     * @throws IllegalArgumentException if no auth token is found in either URL
     */
    constructor(trace: URL, sessionReplay: URL) {
        // Extract token from either URL (prefer trace endpoint)
        this.rumAccessToken = extractAuthToken(trace) ?: extractAuthToken(sessionReplay)
            ?: throw IllegalArgumentException(
                "No auth token found in trace or session replay URLs. Either:\n" +
                    "  1. Use EndpointConfiguration(realm, rumAccessToken) constructor, or\n" +
                    "  2. Include auth parameter in at least one URL: ?auth=YOUR_TOKEN"
            )
        this.traceEndpoint = removeAuthFromUrl(trace)
        this.sessionReplayEndpoint = removeAuthFromUrl(sessionReplay)
    }

    /**
     * Extracts the 'auth' query parameter value from a URL.
     * Example: "https://example.com?auth=ABC123&other=value" -> "ABC123"
     */
    private fun extractAuthToken(url: URL): String? {
        val query = url.query

        if (query == null) {
            Logger.d(TAG, "No query string in URL, auth token not extracted")
            return null
        }

        val token = query.split("&")
            .firstOrNull { it.startsWith("auth=") }
            ?.substringAfter("auth=")
            ?.takeIf { it.isNotBlank() }

        if (token == null) {
            Logger.d(TAG, "No 'auth' parameter found in URL query string")
        }

        return token
    }

    /**
     * Removes the 'auth' query parameter from a URL.
     * Example: "https://example.com?auth=ABC123&other=value" -> "https://example.com?other=value"
     */
    private fun removeAuthFromUrl(url: URL): URL {
        val query = url.query ?: return url

        val cleanedParams = query.split("&")
            .filter { !it.startsWith("auth=") }

        val newQuery = if (cleanedParams.isEmpty()) "" else cleanedParams.joinToString("&")
        val urlString = if (newQuery.isEmpty()) {
            "${url.protocol}://${url.authority}${url.path}"
        } else {
            "${url.protocol}://${url.authority}${url.path}?$newQuery"
        }

        return URL(urlString)
    }

    companion object {
        private const val TAG = "EndpointConfiguration"
    }
}
