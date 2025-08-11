package com.splunk.rum.integration.agent.api.resource

import android.content.Context
import android.os.Build
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.BuildConfig
import com.splunk.rum.integration.agent.internal.id.SimpleId
import com.splunk.rum.utils.extensions.appVersion
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_DESCRIPTION
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION

internal object AgentResource {

    private const val OS_DESCRIPTION_TEMPLATE = "Android Version %s (Build %s API level %s)"
    private const val FALLBACK_VERSION = "0.0.0"

    /**
     * Builds a complete Resource by combining:
     * - Default OpenTelemetry attributes
     * - AgentConfiguration-specific attributes
     * - Device and OS-specific attributes
     */
    internal fun allResource(context: Context, agentConfiguration: AgentConfiguration): Resource =
        Resource
            .getDefault()
            .merge(agentConfigResource(context, agentConfiguration))
            .merge(buildResource())
            .merge(sessionReplayResource())

    private fun agentConfigResource(context: Context, agentConfiguration: AgentConfiguration): Resource =
        Resource.empty().toBuilder()
            .put("app", agentConfiguration.appName)
            .put("app.version", agentConfiguration.appVersion ?: context.appVersion ?: FALLBACK_VERSION)
            .put("deployment.environment", agentConfiguration.deploymentEnvironment)
            .build()

    private fun buildResource(): Resource = Resource.empty().toBuilder()
        .put("rum.sdk.version", BuildConfig.VERSION_NAME)
        .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
        .put(DEVICE_MODEL_NAME, Build.MODEL)
        .put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
        .put(OS_NAME, "Android")
        .put(OS_TYPE, "linux")
        .put(OS_VERSION, Build.VERSION.RELEASE)
        .put(OS_DESCRIPTION, OS_DESCRIPTION_TEMPLATE.format(Build.VERSION.RELEASE, Build.ID, Build.VERSION.SDK_INT))
        .build()

    private fun sessionReplayResource(): Resource = Resource.empty().toBuilder()
        .put("splunk.rumVersion", BuildConfig.VERSION_NAME)
        .put("process.runtime.name", "mobile")
        .put("service.name", "unknown_service")
        .build()
}
