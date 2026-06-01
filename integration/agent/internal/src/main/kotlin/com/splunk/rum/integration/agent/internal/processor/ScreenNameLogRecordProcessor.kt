/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.integration.agent.internal.processor

import com.splunk.rum.common.otel.internal.GlobalRumConstants
import com.splunk.rum.integration.agent.internal.attributes.IScreenNameTracker
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

/**
 * Snapshots the current [IScreenNameTracker.screenName] onto every log record at emit time.
 *
 * Log records are batched before being converted to spans. Without this processor, the
 * global `screen.name` attribute is applied later, at span creation time, when the value
 * may already reflect a subsequent navigation event. By stamping the value here we
 * capture the screen that was active when the event actually occurred.
 *
 * Log records that already carry `screen.name` (e.g. navigation events set it in the
 * emitter) are left untouched.
 */
class ScreenNameLogRecordProcessor(private val screenNameTracker: IScreenNameTracker) : LogRecordProcessor {

    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        val existing = logRecord.toLogRecordData().attributes.get(GlobalRumConstants.SCREEN_NAME_KEY)
        if (existing == null) {
            logRecord.setAttribute(GlobalRumConstants.SCREEN_NAME_KEY, screenNameTracker.screenName)
        }
    }
}
