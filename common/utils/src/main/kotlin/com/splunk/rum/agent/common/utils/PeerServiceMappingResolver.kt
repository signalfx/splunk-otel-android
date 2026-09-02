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
 * Resolves a peer service name from a host, port, and optional request path.
 *
 * Mapping keys use the form `host[:port][/path]`. A path mapping matches request paths by prefix.
 * Invalid mappings are ignored so that a configuration error does not affect network requests.
 */
class PeerServiceMappingResolver(peerServiceMapping: Map<String, String>) {
    private val mappingsByHost: Map<String, List<Mapping>> =
        peerServiceMapping.entries
            .mapNotNull { (peer, serviceName) -> Mapping.parse(peer, serviceName) }
            .groupBy(Mapping::host)

    /** Returns the most specific matching service name, or null when no mapping matches. */
    fun resolve(host: String?, port: Int?, path: String?): String? {
        if (host == null) {
            return null
        }

        return mappingsByHost[host]
            ?.asSequence()
            ?.filter { it.matches(port, path) }
            ?.maxWithOrNull(MAPPING_SPECIFICITY)
            ?.serviceName
    }

    private data class Mapping(val host: String, val port: Int?, val path: String?, val serviceName: String) {
        fun matches(requestPort: Int?, requestPath: String?): Boolean {
            if (port != null && port != requestPort) {
                return false
            }

            if (!path.isNullOrEmpty() && (requestPath == null || !requestPath.startsWith(path))) {
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
                    path = uri.path,
                    serviceName = serviceName
                )
            }.getOrNull()
        }
    }

    private companion object {
        private val MAPPING_SPECIFICITY =
            compareBy<Mapping, Int?>(nullsFirst(naturalOrder())) { it.port }
                .thenBy(nullsFirst(naturalOrder())) { it.path }
    }
}
