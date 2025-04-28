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

package com.splunk.rum.integration.okhttp3

import android.content.Context
import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.internal.AgentIntegration
import com.splunk.rum.integration.agent.internal.extension.find
import com.splunk.rum.integration.agent.module.ModuleConfiguration
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.library.okhttp.v3_0.OkHttpInstrumentation

internal object OkHttp3Integration {

    private const val TAG = "OkHttp3Integration"
    private const val MODULE_NAME = "okHttp3"

    private val defaultModuleConfiguration = OkHttp3ModuleConfiguration()
    private var moduleConfiguration = defaultModuleConfiguration

    init {
        Logger.d(TAG, "init()")
        AgentIntegration.registerModuleInitializationStart(MODULE_NAME)
    }

    fun attach(context: Context) {
        Logger.d(TAG, "attach()")
        AgentIntegration.obtainInstance(context).listeners += installationListener
    }


    private val installationListener = object : AgentIntegration.Listener {
        override fun onInstall(
            context: Context,
            oTelInstallationContext: InstallationContext,
            moduleConfigurations: List<ModuleConfiguration>
        ) {
            Logger.d(TAG, "onInstall()")

            moduleConfiguration = moduleConfigurations.find<OkHttp3ModuleConfiguration>() ?: defaultModuleConfiguration

            //install OkHttp3 auto-instrumentation if isEnabled is true
            if(moduleConfiguration.isEnabled){
                val okHttpInstrumentation = AndroidInstrumentationLoader.getInstrumentation(OkHttpInstrumentation::class.java)
                okHttpInstrumentation?.addAttributesExtractor(OkHttp3AdditionalAttributesExtractor())
                okHttpInstrumentation?.install(oTelInstallationContext)
            }

            AgentIntegration.registerModuleInitializationEnd(MODULE_NAME)
        }
    }
}