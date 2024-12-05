/*
 * Copyright 2024 Splunk Inc.
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

package com.cisco.android.rum.library.httpurlconnection.tracing;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;

public class HttpUrlHttpAttributesGetter
        implements HttpClientAttributesGetter<URLConnection, Integer> {

    @Override
    public String getHttpRequestMethod(URLConnection connection) {
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        return httpURLConnection.getRequestMethod();
    }

    @Override
    public String getUrlFull(URLConnection connection) {
        return connection.getURL().toExternalForm();
    }

    @Override
    public List<String> getHttpRequestHeader(URLConnection connection, String name) {
        String value = connection.getRequestProperty(name);
        return value == null ? emptyList() : singletonList(value);
    }

    @Override
    public Integer getHttpResponseStatusCode(
            URLConnection connection, Integer statusCode, Throwable error) {
        return statusCode;
    }

    @Override
    public List<String> getHttpResponseHeader(
            URLConnection connection, Integer statusCode, String name) {
        String value = connection.getHeaderField(name);
        return value == null ? emptyList() : singletonList(value);
    }

    @Override
    public String getNetworkProtocolName(URLConnection connection, Integer integer) {
        // HttpURLConnection hardcodes the protocol name&version
        return "http";
    }

    @Override
    public String getNetworkProtocolVersion(URLConnection connection, Integer integer) {
        // HttpURLConnection hardcodes the protocol name&version
        return "1.1";
    }

    @Override
    public String getServerAddress(URLConnection connection) {
        return connection.getURL().getHost();
    }

    @Override
    public Integer getServerPort(URLConnection connection) {
        return connection.getURL().getPort();
    }
}

