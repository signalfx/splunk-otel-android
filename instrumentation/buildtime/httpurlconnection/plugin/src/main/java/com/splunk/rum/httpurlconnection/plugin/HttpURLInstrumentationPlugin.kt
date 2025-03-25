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

package com.splunk.rum.httpurlconnection.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

private const val BYTE_BUDDY_GRADLE_PLUGIN_ID = "net.bytebuddy.byte-buddy-gradle-plugin"

/**
 * This plugin adds the [Byte-Buddy Gradle plugin](https://github.com/raphw/byte-buddy/tree/master/byte-buddy-gradle-plugin)
 * and needed dependencies for automatic instrumentation of HTTP request made via HttpURLConnection APIs.
 */
class HttpURLInstrumentationPlugin : Plugin<Project> {

    private lateinit var project: Project
    private val dependenciesVersion: String? = this::class.java.`package`.implementationVersion

    override fun apply(project: Project) {
        this.project = project
        addBytebuddyPluginForHttpURLNRTracing()
        addDependenciesForHttpURLNRTracing()
    }

    private fun addBytebuddyPluginForHttpURLNRTracing() {
        if (!project.pluginManager.hasPlugin(BYTE_BUDDY_GRADLE_PLUGIN_ID)) {
            project.pluginManager.apply(BYTE_BUDDY_GRADLE_PLUGIN_ID)
        }
    }

    private fun addDependenciesForHttpURLNRTracing() {
        project.dependencies.add("byteBuddy", "io.opentelemetry.android:instrumentation-httpurlconnection-agent:$dependenciesVersion")
    }
}