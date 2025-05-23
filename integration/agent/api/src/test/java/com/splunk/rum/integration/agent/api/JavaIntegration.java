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

package com.splunk.rum.integration.agent.api;

import android.app.Application;

import com.splunk.rum.integration.agent.common.module.ModuleConfiguration;

import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;
// TODO will be fixed with JAVA api ticket
//public class JavaIntegration extends Application {
//
//    @Override
//    public void onCreate() {
//        URL url;
//
//        try {
//            url = new URL("https://alameda-eum-qe.saas.appd-test.com");
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }
//
//        AgentConfiguration agentConfig = new AgentConfiguration(url, "smartlook-android", "0.1", true);
//
//        ModuleConfiguration[] moduleConfigs = new ModuleConfiguration[2];
//        moduleConfigs[0] = new CustomModuleConfiguration();
//        moduleConfigs[1] = new CustomModuleConfiguration();
//
//        SplunkRum agent = SplunkRum.install(this, agentConfig, moduleConfigs);
//    }
//
//    private static class CustomModuleConfiguration implements ModuleConfiguration {
//
//        @NotNull
//        @Override
//        public String getName() {
//            return "test";
//        }
//
//        @NotNull
//        @Override
//        public List<Pair<String, String>> getAttributes() {
//            ArrayList<Pair<String, String>> attributes = new ArrayList<>();
//            attributes.add(new Pair<>("enabled", "true"));
//            return attributes;
//        }
//    }
//}
