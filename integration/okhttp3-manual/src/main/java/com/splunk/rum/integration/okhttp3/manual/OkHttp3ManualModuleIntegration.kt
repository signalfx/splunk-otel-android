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

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.common.module.ModuleConfiguration
import com.splunk.rum.integration.agent.internal.module.ModuleIntegration
import com.splunk.rum.integration.okhttp.common.OkHttp3AdditionalAttributesExtractor
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTelemetry

internal object OkHttp3ManualModuleIntegration : ModuleIntegration<OkHttp3ManualModuleConfiguration>(
    defaultModuleConfiguration = OkHttp3ManualModuleConfiguration()
) {
    private const val TAG = "OkHttp3ManualIntegration"

    internal lateinit var okHttpTelemetry: OkHttpTelemetry
        private set

    override fun onInstall(
        context: Context,
        oTelInstallationContext: InstallationContext,
        moduleConfigurations: List<ModuleConfiguration>
    ) {
        Logger.d(TAG, "onInstall()")

        // Setup OkHttp3 manual instrumentation
        okHttpTelemetry = OkHttpTelemetry.builder(oTelInstallationContext.openTelemetry)
            .addAttributesExtractor(OkHttp3AdditionalAttributesExtractor())
            .apply {
                moduleConfiguration.capturedRequestHeaders.takeIf { it.isNotEmpty() }
                    ?.let { setCapturedRequestHeaders(it) }

                moduleConfiguration.capturedResponseHeaders.takeIf { it.isNotEmpty() }
                    ?.let { setCapturedResponseHeaders(it) }
            }
            .build()
    }
}
