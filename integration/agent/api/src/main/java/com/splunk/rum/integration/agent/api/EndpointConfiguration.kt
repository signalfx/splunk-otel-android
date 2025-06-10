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

import java.net.URL

class EndpointConfiguration {

    var traceEndpoint: URL? = null
        private set
    var sessionReplayEndpoint: URL? = null
        private set
    var realm: String? = null
        private set
    var rumAccessToken: String? = null
        private set

    internal constructor()

    /**
     * @param realm Sets the realm for the beacon to send RUM telemetry to, e.g. "us0", "eu0".
     * @param rumAccessToken Sets the RUM auth token to be used by the RUM library.
     */
    constructor(realm: String, rumAccessToken: String) {
        this.realm = realm
        this.rumAccessToken = rumAccessToken
        traceEndpoint = URL("https://rum-ingest.$realm.signalfx.com/v1/rumotlp?auth=$rumAccessToken")
        this.sessionReplayEndpoint = URL("https://rum-ingest.$realm.signalfx.com/v1/rumreplay?auth=$rumAccessToken")
    }

    /**
     * @param trace Sets the "beacon" endpoint URL to be used by the RUM library.
     */
    constructor(trace: URL) {
        traceEndpoint = trace
    }

    /**
     * @param trace Sets the "beacon" endpoint URL to be used by the RUM library.
     * @param sessionReplay Sets the "session replay" endpoint URL to be used by the RUM library.
     */
    constructor(trace: URL, sessionReplay: URL) {
        traceEndpoint = trace
        this.sessionReplayEndpoint = sessionReplay
    }
}
