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
import com.android.build.gradle.api.ApplicationVariant
import com.splunk.rum.mappingfile.plugin.utils.BuildIdInjector
import com.splunk.rum.mappingfile.plugin.utils.MappingFileUploader
import java.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class MappingFilePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var buildIdInjector: BuildIdInjector
    private lateinit var mappingFileUploader: MappingFileUploader
    private lateinit var extension: SplunkRumExtension

    override fun apply(project: Project) {
        this.project = project
        project.logger.lifecycle("Splunk RUM: Mapping File Plugin applied")

        extension = project.extensions.create("splunkRum", SplunkRumExtension::class.java)
        project.logger.debug("Splunk RUM: Created splunkRum extension")

        buildIdInjector = BuildIdInjector(project)
        mappingFileUploader = MappingFileUploader(project)
        project.logger.debug("Splunk RUM: Initialized BuildIdInjector and MappingFileUploader")

        project.afterEvaluate {
            project.logger.debug("Splunk RUM: Evaluating plugin configuration")
            if (extension.enabled.get()) {
                project.logger.info("Splunk RUM: Plugin is enabled, proceeding with setup")
                setupBuildIdInjection()
            } else {
                project.logger.lifecycle("Splunk RUM: Plugin is disabled via configuration")
            }
        }
    }

    private fun setupBuildIdInjection() {
        project.logger.debug("Splunk RUM: Looking for Android application plugin")
        val android = project.extensions.findByType(AppExtension::class.java)
        if (android == null) {
            project.logger.warn("Splunk RUM: Android application plugin not found, skipping setup")
            return
        }

        project.logger.info("Splunk RUM: Found Android application plugin, configuring variants")
        android.applicationVariants.configureEach { variant ->
            project.logger.debug("Splunk RUM: Evaluating variant '${variant.name}'")
            if (variant.buildType.isMinifyEnabled) {
                project.logger.info("Splunk RUM: Processing variant '${variant.name}' (minification enabled)")
                processVariant(variant)
            } else {
                project.logger.info("Splunk RUM: Skipping variant '${variant.name}' as minification not enabled")
            }
        }
    }

    private fun processVariant(variant: ApplicationVariant) {
        project.logger.debug("Splunk RUM: Starting processing for variant '${variant.name}'")

        val buildId = UUID.randomUUID().toString()
        project.logger.lifecycle("Splunk RUM: Generated build ID for variant '${variant.name}': $buildId")
        project.logger.debug("Splunk RUM: Build ID length: ${buildId.length} characters")

        // Inject build ID into manifest
        project.logger.debug("Splunk RUM: Initiating build ID injection for variant '${variant.name}'")
        buildIdInjector.injectBuildId(variant, buildId)

        // Set up upload task
        if (extension.uploadEnabled.get()) {
            project.logger.info("Splunk RUM: Upload is enabled, setting up upload task for variant '${variant.name}'")
            setupUploadTask(variant, buildId)
        } else {
            project.logger.lifecycle("Splunk RUM: Mapping file upload disabled via configuration")
        }
    }

    private fun setupUploadTask(variant: ApplicationVariant, buildId: String) {
        // Hook into the assemble task to upload after build completes
        val assembleTaskName = "assemble${variant.name.capitalize(Locale.ROOT)}"
        project.logger.debug("Splunk RUM: Hooking into task '$assembleTaskName' for upload")

        project.tasks.named(assembleTaskName).configure { assembleTask ->
            assembleTask.doLast {
                project.logger.debug("Splunk RUM: Assemble task completed, starting upload process")
                mappingFileUploader.uploadAfterBuild(variant, buildId, extension)
            }
        }
        project.logger.info("Splunk RUM: Successfully configured upload task for variant '${variant.name}'")
    }
}
