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
import com.splunk.rum.mappingfile.plugin.utils.SplunkLogger
import java.util.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class MappingFilePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var logger: SplunkLogger
    private lateinit var buildIdInjector: BuildIdInjector
    private lateinit var mappingFileUploader: MappingFileUploader
    private lateinit var extension: SplunkRumExtension

    override fun apply(project: Project) {
        this.project = project
        this.logger = SplunkLogger(project.logger)

        logger.lifecycle("Setup", "Mapping File Plugin applied")

        extension = project.extensions.create("splunkRum", SplunkRumExtension::class.java)
        logger.debug("Setup", "Created splunkRum extension")

        buildIdInjector = BuildIdInjector(project)
        mappingFileUploader = MappingFileUploader(project)
        logger.debug("Setup", "Initialized BuildIdInjector and MappingFileUploader")

        project.afterEvaluate {
            logger.debug("Setup", "Evaluating plugin configuration")
            if (extension.enabled.get()) {
                logger.info("Setup", "Plugin is enabled, proceeding with setup")
                setupBuildIdInjection()
            } else {
                logger.lifecycle("Setup", "Plugin is disabled via configuration")
            }
        }
    }

    private fun setupBuildIdInjection() {
        logger.debug("Setup", "Looking for Android application plugin")
        val android = project.extensions.findByType(AppExtension::class.java)
        if (android == null) {
            logger.warn("Setup", "Android application plugin not found, skipping setup")
            return
        }

        logger.info("Setup", "Found Android application plugin, configuring variants")
        android.applicationVariants.configureEach { variant ->
            logger.debug("Setup", "Evaluating variant '${variant.name}'")
            if (variant.buildType.isMinifyEnabled) {
                logger.info("Setup", "Processing variant '${variant.name}' (minification enabled)")
                processVariant(variant)
            } else {
                logger.info("Setup", "Skipping variant '${variant.name}' as minification not enabled")
            }
        }
    }

    private fun processVariant(variant: ApplicationVariant) {
        logger.debug("Setup", "Starting processing for variant '${variant.name}'")

        val buildId = UUID.randomUUID().toString()
        logger.lifecycle("BuildId", "Generated build ID for variant '${variant.name}': $buildId")
        logger.debug("BuildId", "Build ID length: ${buildId.length} characters")

        // Inject build ID into manifest
        logger.debug("BuildId", "Initiating build ID injection for variant '${variant.name}'")
        buildIdInjector.injectBuildId(variant, buildId)

        // Set up upload task
        logger.info("Upload", "Setting up upload task for variant '${variant.name}'")
        setupUploadTask(variant, buildId)
    }

    private fun setupUploadTask(variant: ApplicationVariant, buildId: String) {
        // Hook into the assemble task to upload after build completes
        val assembleTaskName = "assemble${variant.name.capitalize(Locale.ROOT)}"
        logger.debug("Upload", "Hooking into task '$assembleTaskName' for upload")

        project.tasks.named(assembleTaskName).configure { assembleTask ->
            assembleTask.doLast {
                logger.debug("Upload", "Assemble task completed, starting upload process")
                mappingFileUploader.uploadAfterBuild(variant, buildId, extension)
            }
        }
        logger.info("Upload", "Successfully configured upload task for variant '${variant.name}'")
    }
}
