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

package com.splunk.rum.instrumentation.okhttp3.auto.internal

import com.splunk.rum.agent.common.utils.PeerServiceMappingResolver
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter
import java.net.URI
import okhttp3.Interceptor
import okhttp3.Response

/** Extracts the configured `peer.service` mapping for auto-instrumented OkHttp requests. */
internal class PeerServiceAttributesExtractor(
    private val attributesGetter: ServerAttributesGetter<Interceptor.Chain>,
    peerServiceMapping: Map<String, String>
) : AttributesExtractor<Interceptor.Chain, Response> {
    private val resolver = PeerServiceMappingResolver(peerServiceMapping)

    override fun onStart(attributes: AttributesBuilder, parentContext: Context, request: Interceptor.Chain) = Unit

    override fun onEnd(
        attributes: AttributesBuilder,
        context: Context,
        request: Interceptor.Chain,
        response: Response?,
        error: Throwable?
    ) {
        if (resolver.isEmpty()) {
            return
        }

        val serviceName = resolver.resolve(
            attributesGetter.getServerAddress(request),
            attributesGetter.getServerPort(request),
            runCatching { URI(request.request().url.toString()).path }.getOrNull()
        ) ?: return

        attributes.put(PEER_SERVICE, serviceName)
    }

    private companion object {
        private val PEER_SERVICE = AttributeKey.stringKey("peer.service")
    }
}
