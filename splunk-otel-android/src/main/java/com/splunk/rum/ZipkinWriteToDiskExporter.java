package com.splunk.rum;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import zipkin2.reporter.Sender;

public class ZipkinWriteToDiskExporter implements SpanExporter  {

    private final ZipkinSpanExporter delegate;

    // Visible for testing
    ZipkinWriteToDiskExporter(ZipkinSpanExporter delegate) {
        this.delegate = delegate;
    }

    public static ZipkinWriteToDiskExporter create(File path){
        Sender sender = new ZipkinToDiskSender(path);
        ZipkinSpanExporter delegate = ZipkinSpanExporter.builder()
                .setEncoder(new CustomZipkinEncoder())
                .setSender(sender)
                .build();
        return new ZipkinWriteToDiskExporter(delegate);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        return delegate.export(spans);
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
