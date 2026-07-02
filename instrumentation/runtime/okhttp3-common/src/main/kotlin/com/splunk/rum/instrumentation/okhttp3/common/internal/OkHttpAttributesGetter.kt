/*
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

package com.splunk.rum.instrumentation.okhttp3.common.internal

import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter
import java.net.InetSocketAddress
import java.net.SocketAddress
import okhttp3.Interceptor
import okhttp3.Response

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@Suppress("OtelDeprecatedApiUsage") // SPDY_3 is deprecated but still used in okhttp 3.x
enum class OkHttpAttributesGetter : HttpClientAttributesGetter<Interceptor.Chain, Response> {
    INSTANCE;

    override fun getHttpRequestMethod(chain: Interceptor.Chain): String = chain.request().method

    override fun getUrlFull(chain: Interceptor.Chain): String = chain.request().url.toString()

    override fun getHttpRequestHeader(chain: Interceptor.Chain, name: String): List<String> =
        chain.request().headers(name)

    override fun getHttpResponseStatusCode(chain: Interceptor.Chain, response: Response, error: Throwable?): Int? =
        response.code

    override fun getHttpResponseHeader(chain: Interceptor.Chain, response: Response, name: String): List<String> =
        response.headers(name)

    override fun getNetworkProtocolName(chain: Interceptor.Chain, response: Response?): String? {
        if (response == null) {
            return null
        }
        when (response.protocol) {
            okhttp3.Protocol.HTTP_1_0,
            okhttp3.Protocol.HTTP_1_1,
            okhttp3.Protocol.HTTP_2
            -> return "http"
            okhttp3.Protocol.SPDY_3 -> return "spdy"
            else -> {
                // added in 3.11.0
                if ("H2_PRIOR_KNOWLEDGE" == response.protocol.name) {
                    return "http"
                }
            }
        }
        return null
    }

    override fun getNetworkProtocolVersion(chain: Interceptor.Chain, response: Response?): String? {
        if (response == null) {
            return null
        }
        when (response.protocol) {
            okhttp3.Protocol.HTTP_1_0 -> return "1.0"
            okhttp3.Protocol.HTTP_1_1 -> return "1.1"
            okhttp3.Protocol.HTTP_2 -> return "2"
            okhttp3.Protocol.SPDY_3 -> return "3.1"
            else -> {
                // added in 3.11.0
                if ("H2_PRIOR_KNOWLEDGE" == response.protocol.name) {
                    return "2"
                }
            }
        }
        return null
    }

    override fun getServerAddress(chain: Interceptor.Chain): String? = chain.request().url.host

    override fun getServerPort(chain: Interceptor.Chain): Int? = chain.request().url.port

    override fun getNetworkPeerInetSocketAddress(chain: Interceptor.Chain, response: Response?): InetSocketAddress? {
        val connection = chain.connection() ?: return null
        val socketAddress: SocketAddress = connection.socket().remoteSocketAddress
        return socketAddress as? InetSocketAddress
    }
}
