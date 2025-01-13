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

package com.splunk.sdk.common.otel.internal

import android.os.Build
import com.cisco.mrum.common.otel.internal.BuildConfig
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ResourceAttributes.DEVICE_ID
import io.opentelemetry.semconv.ResourceAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.semconv.ResourceAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.semconv.ResourceAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.semconv.ResourceAttributes.OS_DESCRIPTION
import io.opentelemetry.semconv.ResourceAttributes.OS_NAME
import io.opentelemetry.semconv.ResourceAttributes.OS_TYPE
import io.opentelemetry.semconv.ResourceAttributes.OS_VERSION

internal object Resources {

    fun createDefault(deviceId: String): Resource {
        return Resource.getDefault().toBuilder()
            .put(DEVICE_ID, deviceId)
            .put(AttributeKey.stringKey("com.appdynamics.agent.version"), BuildConfig.VERSION_NAME)
            .put(DEVICE_MODEL_NAME, Build.MODEL)
            .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
            .put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
            .put(OS_NAME, "Android")
            .put(OS_VERSION, Build.VERSION.RELEASE)
            .put(OS_DESCRIPTION, getOSDescription())
            .put(OS_TYPE, "linux")
            .build()
    }

    private fun getOSDescription(): String {
        return StringBuilder().apply {
            append("Android Version ")
            append(Build.VERSION.RELEASE)
            append(" (Build ")
            append(Build.ID)
            append(" API level ")
            append(Build.VERSION.SDK_INT)
            append(")")
        }.toString()
    }
}
