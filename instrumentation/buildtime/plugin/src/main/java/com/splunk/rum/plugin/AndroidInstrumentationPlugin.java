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

package com.splunk.rum.plugin;

import net.bytebuddy.build.gradle.android.ByteBuddyAndroidPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * This plugin adds the <a href="https://github.com/raphw/byte-buddy/tree/master/byte-buddy-gradle-plugin">Byte-Buddy Gradle plugin</a>
 *  * and needed dependencies for automatic instrumentation of HTTP request made via HttpURLConnection APIs.
 */
public class AndroidInstrumentationPlugin implements Plugin<Project> {

    private Project project;
    private final String dependenciesVersion = this.getClass().getPackage().getImplementationVersion();

    @Override
    public void apply(Project project) {
        this.project = project;
        addBytebuddyPluginForNRTracing();
        addAutoInstrumentationDependenciesForNRTracing();
    }

    private void addBytebuddyPluginForNRTracing() {
        project.getPluginManager().apply(ByteBuddyAndroidPlugin.class);
    }

    private void addAutoInstrumentationDependenciesForNRTracing() {
        project.getDependencies().add("byteBuddy", "io.opentelemetry.android:instrumentation-httpurlconnection-agent:" + dependenciesVersion);
    }
}