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

package com.cisco.android.rum.plugin;

import com.cisco.android.rum.buildInfoInjector.BuildInfoInjectorPlugin;
import net.bytebuddy.build.gradle.android.ByteBuddyAndroidPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class AndroidInstrumentationPlugin implements Plugin<Project> {

    private Project project;
    private final String dependenciesVersion = this.getClass().getPackage().getImplementationVersion();

    @Override
    public void apply(Project project) {
        this.project = project;
        applyBuildInfoInjectorPlugin(project);
        addBytebuddyPluginForNRTracing();
        addAutoInstrumentationDependenciesForNRTracing();
    }

    private void applyBuildInfoInjectorPlugin(Project project) {
        project.getPluginManager().apply(BuildInfoInjectorPlugin.class);
    }

    private void addBytebuddyPluginForNRTracing() {
        project.getPluginManager().apply(ByteBuddyAndroidPlugin.class);
    }

    private void addAutoInstrumentationDependenciesForNRTracing() {
        project.getDependencies().add("byteBuddy", "com.cisco.android:rum-network-request-bci:" + dependenciesVersion);
        project.getDependencies().add("implementation", "com.cisco.android:rum-network-request-library:" + dependenciesVersion);
    }
}