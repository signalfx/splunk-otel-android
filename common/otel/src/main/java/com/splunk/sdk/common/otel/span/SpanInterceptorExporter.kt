package com.splunk.sdk.common.otel.span

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

internal class SpanInterceptorExporter(
    private val delegate: SpanExporter,
    private val interceptor: ((SpanData) -> SpanData?)?
) : SpanExporter {

    override fun export(spans: MutableCollection<SpanData>): CompletableResultCode {
        val modifiedSpans = spans.mapNotNull {
            if (interceptor != null)
                interceptor.invoke(it)
            else {
                it
            }
        }
        return delegate.export(modifiedSpans)
    }

    override fun flush(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }

    override fun shutdown(): CompletableResultCode {
        return CompletableResultCode.ofSuccess()
    }
}
