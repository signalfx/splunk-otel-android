package com.splunk.rum;

import android.util.Log;

import java.io.File;
import java.util.Locale;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import zipkin2.reporter.Sender;

/**
 * Creates a ZipkinSpanExporter that is configured with an instance of
 * a ZipkinToDiskSender that writes telemetry to disk.
 */
public class ZipkinWriteToDiskExporterFactory {

    private ZipkinWriteToDiskExporterFactory(){
    }

    public static ZipkinSpanExporter create(File path) {
        File spansPath = new File(String.format(Locale.getDefault(), "%s%sspans", path.getAbsolutePath(), File.separator));
        if (!spansPath.exists()) {
            if(!spansPath.mkdirs()){
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
