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

package com.splunk.rum.common.otel

import android.app.Application
import com.splunk.android.common.job.JobIdStorage
import com.splunk.android.common.job.JobManager
import com.splunk.rum.common.otel.logRecord.AndroidLogRecordExporter
import com.splunk.rum.common.otel.span.AndroidSpanExporter
import com.splunk.rum.common.otel.span.SpanInterceptorExporter
import com.splunk.rum.common.storage.AgentStorage
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor

class OpenTelemetryInitializer(
    application: Application,
    deferredUntilForeground: Boolean,
    spanInterceptor: ((SpanData) -> SpanData?)? = null
) {
    private var resource: Resource

    private val spanProcessors: MutableList<SpanProcessor> = mutableListOf()
    private val logRecordProcessors: MutableList<LogRecordProcessor> = mutableListOf()

    init {
        val agentStorage = AgentStorage.attach(application)
        val jobManager = JobManager.attach(application)
        val jobIdStorage = JobIdStorage.init(application, isEncrypted = false)

        resource = Resource.getDefault()

        val spanExporter = SpanInterceptorExporter(
            AndroidSpanExporter(
                agentStorage = agentStorage,
                jobManager = jobManager,
                jobIdStorage = jobIdStorage,
                deferredUntilForeground = deferredUntilForeground,
                context = application.applicationContext
            ),
            spanInterceptor
        )

        spanProcessors += BatchSpanProcessor.builder(spanExporter).build()

        logRecordProcessors += BatchLogRecordProcessor.builder(
            AndroidLogRecordExporter(
                agentStorage = agentStorage,
                jobManager = jobManager,
                jobIdStorage = jobIdStorage
            )
        ).build()
    }

    fun build(global: Boolean = false): OpenTelemetrySdk {
        val instance = OpenTelemetrySdk.builder()
            .setTracerProvider(createTracerProvider())
            .setLoggerProvider(createLoggerProvider())
            .setPropagators(createPropagators())

        val sdk = if (global) instance.buildAndRegisterGlobal() else instance.build()

        SplunkOpenTelemetrySdk.instance = sdk

        return sdk
    }

    fun addSpanProcessor(spanProcessor: SpanProcessor): OpenTelemetryInitializer {
        spanProcessors += spanProcessor
        return this
    }

    fun addLogRecordProcessor(logRecordProcessor: LogRecordProcessor): OpenTelemetryInitializer {
        logRecordProcessors += logRecordProcessor
        return this
    }

    fun joinResources(resource: Resource): OpenTelemetryInitializer {
        this.resource = this.resource.merge(resource)
        return this
    }

    private fun createTracerProvider(): SdkTracerProvider {
        val builder = SdkTracerProvider.builder()
            .setResource(resource)

        spanProcessors.forEach { builder.addSpanProcessor(it) }

        return builder.build()
    }

    private fun createLoggerProvider(): SdkLoggerProvider {
        val builder = SdkLoggerProvider.builder()
            .setResource(resource)

        logRecordProcessors.forEach { builder.addLogRecordProcessor(it) }

        return builder.build()
    }

    private fun createPropagators(): ContextPropagators {
        val propagator = TextMapPropagator.composite(
            W3CTraceContextPropagator.getInstance(),
            W3CBaggagePropagator.getInstance()
        )
        return ContextPropagators.create(propagator)
    }
}
