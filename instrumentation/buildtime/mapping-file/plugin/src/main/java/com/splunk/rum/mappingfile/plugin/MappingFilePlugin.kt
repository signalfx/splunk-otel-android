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

package com.splunk.rum.mappingfile.plugin

import com.android.build.gradle.AppExtension
import com.splunk.rum.mappingfile.plugin.utils.BuildIdInjector
import com.splunk.rum.mappingfile.plugin.utils.MappingFileUploader
import java.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class MappingFilePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var buildIdInjector: BuildIdInjector
    private lateinit var mappingFileUploader: MappingFileUploader

    override fun apply(project: Project) {
        this.project = project
        // TODO: replace all println with more proper logging
        println("Splunk RUM: Mapping File Plugin applied!")

        buildIdInjector = BuildIdInjector(project)
        mappingFileUploader = MappingFileUploader(project)

        project.afterEvaluate {
            setupBuildIdInjection()
        }
    }

    private fun setupBuildIdInjection() {
        val android = project.extensions.findByType(AppExtension::class.java)
        if (android == null) {
            println("Splunk RUM: Android application plugin not found, skipping setup")
            return
        }

        android.applicationVariants.configureEach { variant ->
            if (variant.buildType.isMinifyEnabled) {
                processVariant(variant)
            } else {
                println("Splunk RUM: Skipping variant '${variant.name}' as minification not enabled")
            }
        }
    }

    private fun processVariant(variant: com.android.build.gradle.api.ApplicationVariant) {
        // TODO: check that customer does not have existing manual setup, if so, skip this process and log notice

        val buildId = UUID.randomUUID().toString()
        println("Splunk RUM: Generated build ID for variant '${variant.name}': $buildId")

        // Inject build ID into manifest
        buildIdInjector.injectBuildId(variant, buildId)

        // Set up upload task
        setupUploadTask(variant, buildId)
    }

    private fun setupUploadTask(variant: com.android.build.gradle.api.ApplicationVariant, buildId: String) {
        // Hook into the assemble task to upload after build completes
        val assembleTaskName = "assemble${variant.name.capitalize(Locale.ROOT)}"
        project.tasks.named(assembleTaskName).configure { assembleTask ->
            assembleTask.doLast {
                mappingFileUploader.uploadAfterBuild(variant, buildId)
            }
        }
    }
}
