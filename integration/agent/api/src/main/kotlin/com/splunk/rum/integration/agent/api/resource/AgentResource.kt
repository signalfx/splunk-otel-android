package com.splunk.rum.integration.agent.api.resource

import android.content.Context
import android.os.Build
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.BuildConfig
import com.splunk.rum.utils.extensions.appVersion
import io.opentelemetry.sdk.resources.Resource

internal object AgentResource {

    private const val OS_DESCRIPTION_TEMPLATE = "Android Version %s (Build %s API level %s)"
    private const val FALLBACK_VERSION = "0.0.0"

    private const val KEY_APP = "app"
    private const val KEY_APP_VERSION = "app.version"
    private const val KEY_APP_INSTALLATION_ID = "app.installation.id"
    private const val KEY_DEPLOYMENT_ENVIRONMENT = "deployment.environment"
    private const val KEY_RUM_SDK_VERSION = "rum.sdk.version"
    private const val KEY_DEVICE_ID = "device.id"
    private const val KEY_DEVICE_MODEL_NAME = "device.model.name"
    private const val KEY_DEVICE_MANUFACTURER = "device.manufacturer"
    private const val KEY_OD_NAME = "os.name"
    private const val KEY_OS_TYPE = "os.type"
    private const val KEY_OS_VERSION = "os.version"
    private const val KEY_OS_DESCRIPTION = "os.description"
    private const val KEY_SPLUNK_RUM_VERSION = "splunk.rumVersion"
    private const val KEY_PROCESS_RUNTIME_NAME = "process.runtime.name"
    private const val KEY_SERVICE_NAME = "service.name"

    /**
     * Builds a complete Resource by combining:
     * - Default OpenTelemetry attributes
     * - AgentConfiguration-specific attributes
     * - Device and OS-specific attributes
     */
    internal fun allResource(
        context: Context,
        appInstallationID: String,
        agentConfiguration: AgentConfiguration
    ): Resource = Resource
        .getDefault()
        .merge(agentConfigResource(context, appInstallationID, agentConfiguration))
        .merge(buildResource())
        .merge(sessionReplayResource())

    private fun agentConfigResource(
        context: Context,
        appInstallationID: String,
        agentConfiguration: AgentConfiguration
    ): Resource = Resource.empty().toBuilder()
        .put(KEY_APP, agentConfiguration.appName)
        .put(KEY_APP_VERSION, agentConfiguration.appVersion ?: context.appVersion ?: FALLBACK_VERSION)
        .put(KEY_APP_INSTALLATION_ID, appInstallationID)
        .put(KEY_DEPLOYMENT_ENVIRONMENT, agentConfiguration.deploymentEnvironment)
        .build()

    private fun buildResource(): Resource = Resource.empty().toBuilder()
        .put(KEY_RUM_SDK_VERSION, BuildConfig.VERSION_NAME)
        .put(KEY_DEVICE_ID, Build.MODEL)
        .put(KEY_DEVICE_MODEL_NAME, Build.MODEL)
        .put(KEY_DEVICE_MANUFACTURER, Build.MANUFACTURER)
        .put(KEY_OD_NAME, "Android")
        .put(KEY_OS_TYPE, "linux")
        .put(KEY_OS_VERSION, Build.VERSION.RELEASE)
        .put(KEY_OS_DESCRIPTION, OS_DESCRIPTION_TEMPLATE.format(Build.VERSION.RELEASE, Build.ID, Build.VERSION.SDK_INT))
        .build()

    private fun sessionReplayResource(): Resource = Resource.empty().toBuilder()
        .put(KEY_SPLUNK_RUM_VERSION, BuildConfig.VERSION_NAME)
        .put(KEY_PROCESS_RUNTIME_NAME, "mobile")
        .put(KEY_SERVICE_NAME, "unknown_service")
        .build()
}
