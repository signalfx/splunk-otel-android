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

import android.app.Application
import android.content.Context
import com.cisco.android.common.job.IJobManager
import com.cisco.android.common.job.JobIdStorage
import com.cisco.android.common.utils.AppStateObserver
import com.splunk.sdk.common.storage.IAgentStorage
import io.opentelemetry.exporter.internal.otlp.traces.TraceRequestMarshaler
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * This Exporter is added to Otel by default, it handles the offline/persistance.
 */
internal class AndroidSpanExporter(
    private val agentStorage: IAgentStorage,
    private val jobManager: IJobManager,
    private val jobIdStorage: JobIdStorage,
    private val deferredUntilForeground: Boolean,
    context: Context
) : SpanExporter {
    private val buffer = mutableListOf<SpanData>()
    private val appStateObserver = AppStateObserver()
    private var isForeground = false

    init {
        appStateObserver.listener = AppStateObserverListener()
        appStateObserver.attach(context.applicationContext as Application)
    }

    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode = if (deferredUntilForeground && isForeground) {
            buffer.addAll(spans)
            CompletableResultCode.ofSuccess()
        } else {
            flushBufferedSpans(spans)
            CompletableResultCode.ofSuccess()
        }

    override fun flush(): CompletableResultCode {
        flushBufferedSpans()
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        buffer.clear()
        return CompletableResultCode.ofSuccess()
    }

    private fun flushBufferedSpans(extra: Collection<SpanData> = emptyList()) {
        val allSpans = buffer + extra
        buffer.clear()

        if (allSpans.isEmpty()) return

        val exportRequest = TraceRequestMarshaler.create(allSpans)
        val id = UUID.randomUUID().toString()

        // Save data to our storage.
        ByteArrayOutputStream().use {
            exportRequest.writeBinaryTo(it)
            agentStorage.writeOtelSpanData(id, it.toByteArray())
        }

        // Job scheduling
        jobManager.scheduleJob(UploadOtelSpanData(id, jobIdStorage))
    }

    private inner class AppStateObserverListener : AppStateObserver.Listener {

        override fun onAppStarted() {
            isForeground = true
        }

        override fun onAppForegrounded() {
            isForeground = true
        }

        override fun onAppBackgrounded() {
            isForeground = false
            if (deferredUntilForeground) {
                flushBufferedSpans()
            }
        }
        override fun onAppClosed() {
            isForeground = false
        }
    }
}
