package com.splunk.rum.integration.agent.api.extension

import android.os.Build
import com.splunk.rum.integration.agent.api.BuildConfig
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_DESCRIPTION
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION

internal const val OS_DESCRIPTION_TEMPLATE = "Android Version %s (Build %s API level %s)"

internal fun Resource.addBuildResources(): Resource = this.toBuilder()
    .put("rum.sdk.version", BuildConfig.VERSION_NAME)
    .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
    .put(DEVICE_MODEL_NAME, Build.MODEL)
    .put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
    .put(OS_NAME, "Android")
    .put(OS_TYPE, "linux")
    .put(OS_VERSION, Build.VERSION.RELEASE)
    .put(OS_DESCRIPTION, OS_DESCRIPTION_TEMPLATE.format(Build.VERSION.RELEASE, Build.ID, Build.VERSION.SDK_INT))
    .build()
