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

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class SplunkRumExtension @Inject constructor(objectFactory: ObjectFactory) {

    /**
     * Enable or disable the mapping file plugin functionality.
     * When enabled, generates unique build IDs for each variant, injects the build ID into the intermediate
     * AndroidManifest.XML files of your app, and then uploads mapping files.
     * Default: true
     */
    val enabled: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)

    /**
     * Access token for Splunk API authentication.
     * If not set, will fall back to splunk.accessToken project property.
     * or SPLUNK_ACCESS_TOKEN environment variable
     */
    val apiAccessToken: Property<String> = objectFactory.property(String::class.java)

    /**
     * Splunk realm for API endpoint.
     * If not set, will fall back to splunk.realm project property
     * or SPLUNK_REALM environment variable
     */
    val realm: Property<String> = objectFactory.property(String::class.java)

    /**
     * Whether to fail the build if mapping file cannot be found, missing configuration, or mapping file upload fails.
     * When false, upload failures are logged but don't stop the build.
     * Default: false
     */
    val failBuildOnUploadFailure: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(false)
}
