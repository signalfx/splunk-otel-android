package com.splunk.rum.integration.agent.api.attributes

import android.app.Application
import com.splunk.sdk.common.otel.internal.RumConstants
import com.splunk.sdk.utils.ApplicationInfoUtils
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import kotlin.math.log

class ErrorIdentifierAttributesLogProcessor(application: Application) : LogRecordProcessor {

    private var applicationId: String? = null
    private var versionCode: String? = null
    private var splunkBuildId: String? = null

    init {
        applicationId = ApplicationInfoUtils.retrieveApplicationId(application)
        versionCode = ApplicationInfoUtils.retrieveVersionCode(application)
        splunkBuildId = ApplicationInfoUtils.retrieveSplunkBuildID(application)
    }

    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        if (logRecord.getAttribute(RumConstants.EXCEPTION_STACKTRACE_KEY) != null) {
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
        if (logRecord.instrumentationScopeInfo.name == "io.opentelemetry.crash") {
            logRecord.setAttribute(RumConstants.COMPONENT_KEY, "crash")
        }
    }
}