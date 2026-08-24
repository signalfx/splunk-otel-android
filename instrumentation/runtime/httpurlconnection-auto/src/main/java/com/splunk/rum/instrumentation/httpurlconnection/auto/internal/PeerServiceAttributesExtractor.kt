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
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter
import java.net.URI
import java.net.URLConnection

/**
 * Preserves the programmatic `peer.service` mapping supported by the previous instrumentation API.
 *
 * The replacement upstream service-peer extractor only reads declarative configuration and cannot
 * consume [HttpUrlInstrumentation.setPeerServiceMapping] values. Keeping this extractor local
 * avoids changing the SDK's telemetry schema during the dependency upgrade.
 */
internal class PeerServiceAttributesExtractor(
    private val attributesGetter: HttpClientAttributesGetter<URLConnection, Int>,
    peerServiceMapping: Map<String, String>
) : AttributesExtractor<URLConnection, Int> {
    private val resolver = PeerServiceMappingResolver(peerServiceMapping)

    override fun onStart(attributes: AttributesBuilder, parentContext: Context, request: URLConnection) = Unit

    override fun onEnd(
        attributes: AttributesBuilder,
        context: Context,
        request: URLConnection,
        response: Int?,
        error: Throwable?
    ) {
        if (resolver.isEmpty()) {
            return
        }

        val serviceName = resolver.resolve(
            attributesGetter.getServerAddress(request),
            attributesGetter.getServerPort(request),
            attributesGetter.getUrlFull(request).toPathOrNull()
        ) ?: return

        attributes.put(PEER_SERVICE, serviceName)
    }

    private class PeerServiceMappingResolver(peerServiceMapping: Map<String, String>) {
        private val mappingsByHost: Map<String, List<Mapping>> =
            peerServiceMapping.entries
                .mapNotNull { (peer, serviceName) -> Mapping.parse(peer, serviceName) }
                .groupBy(Mapping::host)

        fun isEmpty(): Boolean = mappingsByHost.isEmpty()

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
    }

    private data class Mapping(val host: String, val port: Int?, val path: String?, val serviceName: String) {
        fun matches(requestPort: Int?, requestPath: String?): Boolean {
            if (port != null && port != requestPort) {
                return false
            }

            if (!path.isNullOrEmpty()) {
                if (requestPath == null || !requestPath.startsWith(path)) {
                    return false
                }
                if (requestPort != null && requestPort != port) {
                    return false
                }
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
        private val PEER_SERVICE = AttributeKey.stringKey("peer.service")

        private val MAPPING_SPECIFICITY =
            compareBy<Mapping, Int?>(nullsFirst(naturalOrder())) { it.port }
                .thenBy(nullsFirst(naturalOrder())) { it.path }

        private fun String?.toPathOrNull(): String? = this?.let { url -> runCatching { URI(url).path }.getOrNull() }
    }
}
