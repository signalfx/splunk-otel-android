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
     * If the URL contains an 'auth' query parameter, it will be extracted for use in the X-SF-Token header.
     */
    constructor(trace: URL) {
        this.rumAccessToken = extractAuthToken(trace)
        this.traceEndpoint = trace

        if (rumAccessToken == null) {
            Logger.w(
                TAG,
                "No auth token found in trace URL. Requests may fail authentication. " +
                    "Consider using EndpointConfiguration(realm, rumAccessToken) constructor."
            )
        }
    }

    /**
     * @param trace Sets the "beacon" endpoint URL to be used by the RUM library.
     * @param sessionReplay Sets the "session replay" endpoint URL to be used by the RUM library.
     * If either URL contains an 'auth' query parameter, it will be extracted for use in the X-SF-Token header.
     */
    constructor(trace: URL, sessionReplay: URL) {
        // Extract token from either URL (prefer trace endpoint)
        this.rumAccessToken = extractAuthToken(trace) ?: extractAuthToken(sessionReplay)
        this.traceEndpoint = trace
        this.sessionReplayEndpoint = sessionReplay
        if (rumAccessToken == null) {
            Logger.w(
                TAG,
                "No auth token found in URLs. Requests may fail authentication. " +
                    "Consider using EndpointConfiguration(realm, rumAccessToken) constructor."
            )
        }
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

    companion object {
        private const val TAG = "EndpointConfiguration"
    }
}
