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

package com.splunk.rum.integration.agent.api.extension

import android.os.Build
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.BuildConfig
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION

internal fun AgentConfiguration.toResource(): Resource = Resource.getDefault().toBuilder()
    .put("app", appName)
    .put("app.version", requireNotNull(appVersion))
    .put("deployment.environment", deploymentEnvironment)
    .put("rum.sdk.version", BuildConfig.VERSION_NAME)
    .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
    .put(DEVICE_MODEL_NAME, Build.MODEL)
    .put(OS_NAME, "Android")
    .put(OS_TYPE, "linux")
    .put(OS_VERSION, Build.VERSION.SDK_INT.toString())
    .build()
