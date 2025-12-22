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

package com.splunk.rum.common.otel.http

import com.splunk.android.common.http.model.Header
import com.splunk.android.common.logger.Logger
import com.splunk.rum.common.storage.IAgentStorage

/**
 * Utility object for building HTTP headers with authentication for telemetry uploads.
 */
internal object AuthHeaderBuilder {

    /**
     * Builds a list of HTTP headers including authentication if a token is available.
     *
     * @param storage The storage instance to read the rumAccessToken from
     * @param logTag The tag to use for logging (typically the class name)
     * @return A list of headers including Content-Type and X-SF-Token (if token available)
     */
    fun buildHeaders(storage: IAgentStorage, logTag: String): List<Header> {
        val token = storage.readRumAccessToken()
        val headers = mutableListOf(Header("Content-Type", "application/x-protobuf"))

        if (token != null) {
            headers.add(Header("X-SF-Token", token))
            Logger.d(logTag, "Adding X-SF-Token header for authentication")
        } else {
            Logger.w(
                logTag,
                "No rumAccessToken found in storage. Request may fail authentication. " +
                    "Ensure EndpointConfiguration was created with a valid token."
            )
        }

        return headers
    }
}
