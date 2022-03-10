package com.splunk.rum;

import android.util.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import zipkin2.reporter.Sender;

/**
 * Creates a ZipkinSpanExporter that is configured with an instance of
 * a ZipkinToDiskSender that writes telemetry to disk.
 */
public class ZipkinWriteToDiskExporterFactory {

    private ZipkinWriteToDiskExporterFactory(){
    }

    public static ZipkinSpanExporter create(Path path) {
        Path spansPath = path.resolve("spans");
        if (!Files.exists(spansPath)) {
            try {
                Files.createDirectory(spansPath);
            } catch (Exception e) {
                Log.e(SplunkRum.LOG_TAG, "Error creating path " + spansPath + " for span buffer, defaulting to parent");
                spansPath = path;
            }
        }

        Sender sender = new ZipkinToDiskSender(spansPath);
        return ZipkinSpanExporter.builder()
                .setEncoder(new CustomZipkinEncoder())
                .setSender(sender)
                .build();
    }
}
