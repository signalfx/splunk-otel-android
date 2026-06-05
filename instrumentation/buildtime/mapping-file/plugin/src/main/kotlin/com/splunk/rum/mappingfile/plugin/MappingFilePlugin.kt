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

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.VariantOutputConfiguration
import com.android.build.gradle.AppPlugin
import com.splunk.rum.mappingfile.plugin.utils.SplunkLogger
import org.gradle.api.Plugin
import org.gradle.api.Project

class MappingFilePlugin : Plugin<Project> {

    private lateinit var project: Project
    private lateinit var logger: SplunkLogger
    private lateinit var extension: SplunkRumExtension

    override fun apply(project: Project) {
        this.project = project
        this.logger = SplunkLogger(project.logger)

        logger.lifecycle("Setup", "Mapping File Plugin applied")

        extension = project.extensions.create("splunkRum", SplunkRumExtension::class.java)
        logger.debug("Setup", "Created splunkRum extension")

        project.plugins.withType(AppPlugin::class.java) {
            logger.debug("Setup", "Found Android application plugin")

            val androidComponents = project.extensions.getByType(
                ApplicationAndroidComponentsExtension::class.java
            )

            androidComponents.onVariants { variant ->
                if (!extension.enabled.get()) {
                    logger.lifecycle("Setup", "Plugin is disabled via configuration")
                    return@onVariants
                }

                if (variant.isMinifyEnabled) {
                    logger.info("Setup", "Processing variant '${variant.name}' (minification enabled)")
                    processVariant(variant)
                } else {
                    logger.info("Setup", "Skipping variant '${variant.name}' as minification not enabled")
                }
            }
        }
    }

    private fun processVariant(variant: ApplicationVariant) {
        val variantName = variant.name
        val buildTypeName = variant.buildType ?: "unknown"

        logger.debug("Setup", "Starting processing for variant '$variantName'")
        logger.info("Setup", "Setting up build ID injection and upload for variant '$variantName'")

        setupBuildIdInjectionForVariant(variant, variantName)

        setupUploadTask(variant, variantName, buildTypeName)
    }

    private fun setupBuildIdInjectionForVariant(variant: ApplicationVariant, variantName: String) {
        logger.debug("BuildId", "Registering manifest transform task for variant '$variantName'")

        val manifestInjector = project.tasks.register(
            "splunkInjectBuildId${variantName.replaceFirstChar { it.uppercase() }}",
            ManifestBuildIdInjectorTask::class.java
        ) {
            it.variantName.set(variantName)
            it.buildDirectory.set(project.layout.buildDirectory)
            it.outputs.upToDateWhen { false }
        }

        variant.artifacts.use(manifestInjector)
            .wiredWithFiles(
                ManifestBuildIdInjectorTask::mergedManifest,
                ManifestBuildIdInjectorTask::updatedManifest
            )
            .toTransform(SingleArtifact.MERGED_MANIFEST)

        logger.info("BuildId", "Registered manifest transform for variant '$variantName'")
    }

    private fun setupUploadTask(variant: ApplicationVariant, variantName: String, buildTypeName: String) {
        val assembleTaskName = "assemble${variantName.replaceFirstChar { it.uppercase() }}"
        logger.debug("Upload", "Registering upload task for variant '$variantName'")

        val mainOutput = variant.outputs.firstOrNull {
            it.outputType == VariantOutputConfiguration.OutputType.SINGLE
        } ?: variant.outputs.firstOrNull()

        val uploadTask = project.tasks.register(
            "splunkUploadMappingFile${variantName.replaceFirstChar { it.uppercase() }}",
            MappingFileUploadTask::class.java
        ) {
            it.variantName.set(variantName)
            it.applicationId.set(variant.applicationId)
            if (mainOutput != null) {
                it.versionCode.set(mainOutput.versionCode)
            }
            it.buildTypeName.set(buildTypeName)
            it.buildDirectory.set(project.layout.buildDirectory)
            it.failBuildOnUploadFailure.set(extension.failBuildOnUploadFailure)
            it.accessToken.set(
                extension.apiAccessToken.orElse(
                    project.providers.environmentVariable("SPLUNK_ACCESS_TOKEN")
                )
            )
            it.realm.set(
                extension.realm.orElse(
                    project.providers.environmentVariable("SPLUNK_REALM")
                )
            )
        }

        project.tasks.configureEach { task ->
            if (task.name == assembleTaskName) {
                task.finalizedBy(uploadTask)
            }
        }

        logger.info("Upload", "Registered upload task for variant '$variantName'")
    }
}
