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
     * Enable or disable the mapping file plugin functionality. Will generate an unique Splunk Build ID for each variant
     * to accurately correlate stacktraces with mapping files. The gradle plugin will also inject it into the intermediate
     * AndroidManifest.xml files of your app during the build process, and also uploaded with each mapping file
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
     * Enable or disable automatic mapping file upload. Will not work if enabled flag is set to false.
     * Default: true
     */
    val uploadEnabled: Property<Boolean> = objectFactory.property(Boolean::class.java).convention(true)
}
