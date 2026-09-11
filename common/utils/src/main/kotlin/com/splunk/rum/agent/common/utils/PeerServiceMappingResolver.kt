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

package com.splunk.rum.agent.common.utils

import java.net.URI

/**
 * Resolves a peer service name from a host and port.
 *
 * Mapping keys use the form `host[:port]`. Path-qualified mappings are ignored because peer-service
 * resolution is based only on the server address and port across supported HTTP clients. Invalid
 * mappings are ignored so that a configuration error does not affect network requests.
 */
class PeerServiceMappingResolver(peerServiceMapping: Map<String, String>) {
    private val mappingsByHost: Map<String, List<Mapping>> =
        peerServiceMapping.entries
            .mapNotNull { (peer, serviceName) -> Mapping.parse(peer, serviceName) }
            .groupBy(Mapping::host)

    /** Returns true when the mapping contains no valid entries. */
    fun isEmpty(): Boolean = mappingsByHost.isEmpty()

    /** Returns the most specific matching service name, or null when no mapping matches. */
    fun resolve(host: String?, port: Int?): String? {
        if (host == null) {
            return null
        }

        val requestPort = port?.takeIf { it >= 0 }

        return mappingsByHost[host]
            ?.asSequence()
            ?.filter { it.matches(requestPort) }
            ?.maxWithOrNull(MAPPING_SPECIFICITY)
            ?.serviceName
    }

    private data class Mapping(val host: String, val port: Int?, val serviceName: String) {
        fun matches(requestPort: Int?): Boolean {
            if (port != null && port != requestPort) {
                return false
            }
            return true
        }

        companion object {
            fun parse(peer: String, serviceName: String): Mapping? = runCatching {
                val uri = URI("https://$peer")
                Mapping(
                    host = uri.host ?: return null,
                    port = uri.port.takeIf { it >= 0 },
                    serviceName = serviceName
                ).takeIf { uri.path.isNullOrEmpty() }
            }.getOrNull()
        }
    }

    companion object {
        private val MAPPING_SPECIFICITY =
            compareBy<Mapping, Int?>(nullsFirst(naturalOrder())) { it.port }
    }
}
