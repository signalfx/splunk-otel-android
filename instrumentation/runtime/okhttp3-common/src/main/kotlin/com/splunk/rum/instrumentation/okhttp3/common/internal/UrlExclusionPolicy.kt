/*
 * Copyright 2026 Splunk Inc.
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

import io.opentelemetry.context.Context
import io.opentelemetry.context.ContextKey
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Request

/**
 * Internal runtime bridge shared by a module's network and connection failure interceptors.
 * This class is public only for use across the OkHttp runtime modules.
 */
class UrlExclusionPolicy(private val isExcludedUrl: (String) -> Boolean) {
    /** Adds a fresh flag for this call. */
    internal fun withNetworkInterceptorTracking(parentContext: Context): Context =
        parentContext.with(networkInterceptorReached, AtomicBoolean())

    /** Records that the network interceptor ran, then checks this exchange's URL. */
    internal fun markNetworkInterceptorReachedAndCheckExclusion(context: Context, request: Request): Boolean {
        context[networkInterceptorReached]?.set(true)
        return isExcludedUrl(request.url.toString())
    }

    /** Allows the connection error span only if no network exchange was seen and the original URL is allowed. */
    internal fun allowConnectionErrorSpan(context: Context, request: Request): Boolean =
        context[networkInterceptorReached]?.get() != true && !isExcludedUrl(request.url.toString())

    companion object {
        private val networkInterceptorReached =
            ContextKey.named<AtomicBoolean>("splunk-okhttp-networkInterceptorReachedKey")
    }
}
