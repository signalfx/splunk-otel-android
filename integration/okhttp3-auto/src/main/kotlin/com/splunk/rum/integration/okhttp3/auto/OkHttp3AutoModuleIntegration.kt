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

package com.splunk.rum.integration.okhttp3.auto

import android.content.Context
import com.splunk.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.okhttp3.common.OkHttp3AdditionalAttributesExtractor
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.library.okhttp.v3_0.OkHttpInstrumentation
import okhttp3.Interceptor
import okhttp3.Response

internal object OkHttp3AutoModuleIntegration : ModuleIntegration<OkHttp3AutoModuleConfiguration>(
    defaultModuleConfiguration = OkHttp3AutoModuleConfiguration()
) {

    private const val TAG = "OkHttp3Integration"

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        // install OkHttp3 auto-instrumentation if it is enabled
        if (moduleConfiguration.isEnabled) {
            OkHttpInstrumentation().apply {
                addAttributesExtractor(
                    OkHttp3AdditionalAttributesExtractor() as AttributesExtractor<Interceptor.Chain, Response>
                )

                moduleConfiguration.capturedRequestHeaders
                    .takeIf { it.isNotEmpty() }
                    ?.let { setCapturedRequestHeaders(it) }

                moduleConfiguration.capturedResponseHeaders
                    .takeIf { it.isNotEmpty() }
                    ?.let { setCapturedResponseHeaders(it) }

                install(oTelInstallationContext)
            }
        }
    }
}
