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

package com.splunk.app

import android.app.Application
import com.cisco.android.instrumentation.recording.core.api.RenderingMode
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.SplunkRUMAgent
import com.splunk.rum.integration.interactions.InteractionsModuleConfiguration
import com.splunk.rum.integration.sessionreplay.api.sessionReplay
import java.net.URL

class App : Application() {
    lateinit var agent: SplunkRUMAgent
        private set

    override fun onCreate() {
        super.onCreate()

        // TODO: Reenable with the bridge support
        // BridgeManager.bridgeInterfaces += TomasBridgeInterface()

        val agentConfig = AgentConfiguration(
            url = URL("https://alameda-eum-qe.saas.appd-test.com"),
            appName = "smartlook-android",
            appVersion = "0.1",
            isDebugLogsEnabled = true,
        )

        agent = SplunkRUMAgent.install(
            application = this,
            agentConfiguration = agentConfig,
            moduleConfigurations = arrayOf(
                InteractionsModuleConfiguration(
                    isEnabled = true
                )
            )
        )

        // MARK temp comment
        agent.sessionReplay.preferences.renderingMode = RenderingMode.NATIVE
        agent.sessionReplay.start()
    }
}
