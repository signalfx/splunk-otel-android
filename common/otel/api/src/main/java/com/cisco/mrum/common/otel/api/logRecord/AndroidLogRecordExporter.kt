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

package com.cisco.mrum.common.otel.api.logRecord

import com.splunk.sdk.common.job.IJobManager
import com.splunk.sdk.common.job.JobIdStorage
import com.splunk.sdk.common.storage.IStorage
import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.util.UUID

/**
 * This Exporter is added to Otel by default, it handles the offline/persistance.
 */
internal class AndroidLogRecordExporter(
    private val storage: IStorage,
    private val jobManager: IJobManager,
    private val jobIdStorage: JobIdStorage
) : LogRecordExporter {

    override fun export(logs: MutableCollection<LogRecordData>): CompletableResultCode {
        val exportRequest = LogsRequestMarshaler.create(logs)

        val id = UUID.randomUUID().toString()

        // Save data to our storage.
        storage.createOtelLogDataFile(id).outputStream().buffered().use {
            exportRequest.writeBinaryTo(it)
            it.flush()
        }

        // Job scheduling
        jobManager.scheduleJob(UploadOtelLogRecordData(id, jobIdStorage))

        // TODO move this to proper standalone exporter
        // flush to std out too (temporary for demo purposes, uncomment to enable)
        // flushToConsole(logs)

        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    private fun flushToConsole(logs: MutableCollection<LogRecordData>) {
        val stringBuilder = StringBuilder(60)

        for (log in logs) {
            stringBuilder.setLength(0)
            formatLog(stringBuilder, log)
            println(stringBuilder)
        }
    }

    // temporary for demo purposes
    private fun formatLog(stringBuilder: java.lang.StringBuilder, log: LogRecordData) {
        val instrumentationScopeInfo = log.instrumentationScopeInfo
        stringBuilder.append("DEMO log to std out:")
        stringBuilder.appendLine()
        stringBuilder
            .append("SEVERITY: " + log.severity)
            .appendLine()
            .append("BODY: " + log.body.asString())
            .appendLine()
            .append("SPAN CONTEXT TRACE ID: " + log.spanContext.traceId)
            .appendLine()
            .append("SPAN CONTEXT SPAN ID: " + log.spanContext.spanId)
            .appendLine()
            .append("ATTRIBUTES: " + log.attributes.toString())
            .appendLine()
            .append("SCOPE INFO: ")
            .append(instrumentationScopeInfo.name)
            .append(" : ")
            .append(
                if (instrumentationScopeInfo.version == null) "" else instrumentationScopeInfo.version
            )
    }
}
