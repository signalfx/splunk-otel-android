package com.splunk.rum.integration.agent.api.attributes

import android.app.Application
import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.utils.ApplicationInfoUtils
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

class ErrorIdentifierAttributesLogProcessor(application: Application) : LogRecordProcessor {

    private var applicationId: String? = ApplicationInfoUtils.retrieveApplicationId(application)
    private var versionCode: String? = ApplicationInfoUtils.retrieveVersionCode(application)
    private var splunkBuildId: String? = ApplicationInfoUtils.retrieveSplunkBuildID(application)

    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        if (logRecord.getAttribute(RumConstants.COMPONENT_KEY) == RumConstants.COMPONENT_ERROR ||
            logRecord.getAttribute(RumConstants.COMPONENT_KEY) == RumConstants.COMPONENT_CRASH) {
            applicationId?.let {
                logRecord.setAttribute(RumConstants.APPLICATION_ID_KEY, it)
            }
            versionCode?.let {
                logRecord.setAttribute(RumConstants.APP_VERSION_CODE_KEY, it)
            }
            splunkBuildId?.let {
                logRecord.setAttribute(RumConstants.SPLUNK_BUILD_ID, it)
            }
        }
    }
}