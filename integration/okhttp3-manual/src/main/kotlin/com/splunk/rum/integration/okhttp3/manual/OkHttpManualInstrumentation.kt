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

package com.splunk.rum.integration.okhttp3.manual

import com.splunk.android.common.logger.Logger
import okhttp3.Call
import okhttp3.Call.Factory
import okhttp3.OkHttpClient

class OkHttpManualInstrumentation internal constructor() {

    /**
     * Wrap the provided [OkHttpClient] with Splunk RUM instrumentation. Since
     * [Call.Factory] is the primary useful interface implemented by the OkHttpClient, this
     * should be a drop-in replacement for any usages of OkHttpClient.
     *
     * @param client The [OkHttpClient] to wrap with Splunk RUM instrumentation.
     * @return A [Call.Factory] implementation.
     */
    fun buildOkHttpCallFactory(client: OkHttpClient): Factory {
        val okHttpTelemetry = OkHttp3ManualModuleIntegration.okHttpTelemetry

        if (okHttpTelemetry == null) {
            Logger.w(TAG, "OkHttp3 manual instrumentation is not initialized. Check 'SplunkRum.instance.state.status'.")
            return client
        }

        return okHttpTelemetry.newCallFactory(client)
    }

    companion object {

        private const val TAG = "OkHttpManualInstrumentation"

        /**
         * The instance of [OkHttpManualInstrumentation].
         */
        @JvmStatic
        val instance by lazy { OkHttpManualInstrumentation() }
    }
}
