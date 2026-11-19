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

package com.splunk.rum.instrumentation.okhttp3.auto

import com.splunk.rum.instrumentation.okhttp3.auto.internal.OkHttpSingletons
import com.splunk.rum.instrumentation.okhttp3.testing.OkHttpUrlExclusionContract
import io.opentelemetry.api.OpenTelemetry
import okhttp3.Call
import okhttp3.OkHttpClient

class OkHttpUrlExclusionTest : OkHttpUrlExclusionContract() {
    override fun instrument(
        client: OkHttpClient,
        openTelemetry: OpenTelemetry,
        exclusions: ((String) -> Boolean)?
    ): Call.Factory {
        OkHttpInstrumentation().apply { urlExclusionPredicate = exclusions }.install(openTelemetry)
        // Match the ordering used by the build-time OkHttpClientAdvice.
        return client.newBuilder()
            .addInterceptor(OkHttpSingletons.callbackContextInterceptor)
            .addInterceptor(OkHttpSingletons.resendCountContextInterceptor)
            .addInterceptor(OkHttpSingletons.connectionErrorInterceptor)
            .addNetworkInterceptor(OkHttpSingletons.tracingInterceptor)
            .build()
    }
}
