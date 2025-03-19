package com.splunk.rum.integration.agent.api.user

import com.cisco.android.common.logger.Logger
import com.splunk.rum.integration.agent.api.attributes.AttributeConstants.USER_ID_KEY
import com.splunk.rum.integration.agent.internal.user.IUserManager
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

internal class UserIdLogProcessor(private val userManager: IUserManager) : LogRecordProcessor {
    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        val userId = userManager.userId ?: return
        Logger.d("UserIdLogProcessor", "onEmit userId: $userId, ${logRecord.toLogRecordData().attributes}")
        logRecord.setAttribute(USER_ID_KEY, userId)
    }
}