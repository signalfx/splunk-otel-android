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

package com.splunk.sdk.common.otel.span

import com.cisco.android.common.job.IJobManager
import com.cisco.android.common.job.JobIdStorage
import com.splunk.sdk.common.storage.IAgentStorage
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.util.UUID

/**
 * This Exporter is added to Otel by default, it handles the offline/persistance.
 */
internal class AndroidSpanExporter(
    private val agentStorage: IAgentStorage,
    private val jobManager: IJobManager,
    private val jobIdStorage: JobIdStorage
) : SpanExporter {

    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
        val exportRequest = TraceRequestMarshaler.create(spans)

        val id = UUID.randomUUID().toString()

        // Save data to our storage.
        agentStorage.writeOtelSpanData(id).outputStream().buffered().use {
            exportRequest.writeBinaryTo(it)
            it.flush()
        }

        // Job scheduling
        jobManager.scheduleJob(UploadOtelSpanData(id, jobIdStorage))

        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }
}
