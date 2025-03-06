package com.splunk.rum.integration.agent.api

import java.net.URL

class EndpointConfiguration internal constructor() {
    var tracesEndpoint: URL? = null
        private set
    var sessionReplayEndpoint: URL? = null
        private set
    var realm: String? = null
        private set

    constructor(realm: String) : this() {
        tracesEndpoint = URL("https://rum-ingest.$realm.signalfx.com/v1/rumotlp")
        this.sessionReplayEndpoint = URL("https://rum-ingest.$realm.signalfx.com/v1/rumreplay")
    }

    constructor(traces: URL) : this() {
        tracesEndpoint = traces
    }

    constructor(traces: URL, sessionReplayEndpoint: URL) : this() {
        tracesEndpoint = traces
        this.sessionReplayEndpoint = sessionReplayEndpoint
    }
}