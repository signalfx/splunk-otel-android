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

package com.cisco.android.rum.integration.agent.api;

import android.app.Application;

import com.cisco.android.rum.integration.agent.module.ModuleConfiguration;
import com.cisco.android.rum.integration.agent.api.AgentConfiguration;
import com.cisco.android.rum.integration.agent.api.CiscoRUMAgent;

import java.net.MalformedURLException;
import java.net.URL;

public class JavaIntegration extends Application {

    @Override
    public void onCreate() {
        URL url;

        try {
            url = new URL("https://alameda-eum-qe.saas.appd-test.com");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        AgentConfiguration agentConfig = new AgentConfiguration(url, "smartlook-android", "0.1");

        ModuleConfiguration[] moduleConfigs = new ModuleConfiguration[2];
        moduleConfigs[0] = new CustomModuleConfiguration();
        moduleConfigs[1] = new CustomModuleConfiguration();

        CiscoRUMAgent agent = CiscoRUMAgent.install(this, agentConfig, moduleConfigs);
    }

    private static class CustomModuleConfiguration implements ModuleConfiguration {
    }
}
