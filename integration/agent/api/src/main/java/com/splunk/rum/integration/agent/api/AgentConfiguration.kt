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

package com.splunk.rum.integration.agent.api

import java.net.URL

/**
 * Configuration parameters for the RUM agent.
 *
 * @property url The base URL of the server to which RUM data will be sent.
 * @property appName Optional string that contains the name of the application.
 *                  The default value corresponds to `Context.getPackageName()`.
 * @property appVersion Optional string that contains the current application version.
 *                      The default value corresponds to `PackageInfo.versionName`.
 * @property isDebugLogsEnabled Optional boolean that decides if debug logs are visible or not.
 *                              The default value is `false`.
 */
data class AgentConfiguration(
    val url: URL,
    var appName: String? = null,
    var appVersion: String? = null,
    var isDebugLogsEnabled: Boolean = false, // temporary name till product decides on more suitable one
)
