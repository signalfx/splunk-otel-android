package com.splunk.rum;

import java.util.Collection;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;

public class DiskSpanExporter implements SpanExporter  {

    private final BytesEncoder<Span> encoder;

    public DiskSpanExporter(BytesEncoder<Span> encoder) {
        this.encoder = encoder;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        for (SpanData span : spans) {
            span.tospan
        }
        return null;
    }

    @Override
    public CompletableResultCode flush() {
        return null;
    }

    @Override
    public CompletableResultCode shutdown() {
        return null;
    }
}
