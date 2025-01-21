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

package com.splunk.rum.integration.agent.api.state

import com.splunk.rum.integration.agent.api.attributes.AttributeConstants.STATE
import com.splunk.rum.integration.agent.internal.state.StateManager
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

internal class StateLogRecordProcessor(private val stateManager: StateManager) : LogRecordProcessor {
    override fun onEmit(context: Context, logRecord: ReadWriteLogRecord) {
        logRecord.setAttribute(STATE, stateManager.state.value)
    }
}
