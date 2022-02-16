package com.splunk.rum;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import zipkin2.Call;
import zipkin2.codec.Encoding;
import zipkin2.reporter.Sender;

public class ZipkinToDiskSender extends Sender {

    private final Path path;

    public ZipkinToDiskSender(Path path) {
        this.path = path;
    }

    @Override
    public Encoding encoding() {
        return Encoding.JSON;
    }

    @Override
    public int messageMaxBytes() {
        return 1024*1024;
    }

    @Override
    public int messageSizeInBytes(List<byte[]> encodedSpans) {
        return encoding().listSizeInBytes(encodedSpans);
    }

    @Override
    public Call<Void> sendSpans(List<byte[]> encodedSpans) {
        long now = System.currentTimeMillis();
        String outfile = path.toString() + File.pathSeparator + now + ".spans";
        try (FileOutputStream out = new FileOutputStream(outfile)) {
            for (byte[] encodedSpan : encodedSpans) {
                out.write(encodedSpan);
            }
        } catch (IOException e) {
            Log.e(SplunkRum.LOG_TAG, "Error buffering span data to disk", e);
        }
        return Call.create(null);
    }
}
