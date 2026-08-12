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

import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter
import java.net.HttpURLConnection
import java.net.URLConnection

internal object HttpUrlHttpAttributesGetter : HttpClientAttributesGetter<URLConnection, Int> {
    override fun getHttpRequestMethod(connection: URLConnection): String {
        val httpURLConnection = connection as HttpURLConnection
        return httpURLConnection.requestMethod
    }

    override fun getUrlFull(connection: URLConnection): String = connection.url.toExternalForm()

    override fun getHttpRequestHeader(connection: URLConnection, name: String): List<String> {
        val value = connection.getRequestProperty(name)
        return if (value == null) emptyList() else listOf(value)
    }

    override fun getHttpResponseStatusCode(connection: URLConnection, statusCode: Int, error: Throwable?): Int =
        statusCode

    override fun getHttpResponseHeader(connection: URLConnection, statusCode: Int, name: String): List<String> {
        val value = connection.getHeaderField(name)
        return if (value == null) emptyList() else listOf(value)
    }

    override fun getNetworkProtocolName(connection: URLConnection, integer: Int?): String? = "http"

    override fun getNetworkProtocolVersion(connection: URLConnection, integer: Int?): String? = "1.1"

    override fun getServerAddress(connection: URLConnection): String = connection.url.host

    override fun getServerPort(connection: URLConnection): Int = connection.url.port
}
