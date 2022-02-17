package com.splunk.rum;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import zipkin2.Call;
import zipkin2.codec.Encoding;
import zipkin2.reporter.Sender;

public class ZipkinToDiskSender extends Sender {

    private final File path;
    private final FileUtils fileUtils;

    public ZipkinToDiskSender(File path) {
        this(path, new FileUtils());
    }

    // exists for testing
    ZipkinToDiskSender(File path, FileUtils fileUtils) {
        this.path = path;
        this.fileUtils = fileUtils;
    }

    @Override
    public Encoding encoding() {
        return Encoding.JSON;
    }

    @Override
    public int messageMaxBytes() {
        return 1024 * 1024;
    }

    @Override
    public int messageSizeInBytes(List<byte[]> encodedSpans) {
        return encoding().listSizeInBytes(encodedSpans);
    }

    @Override
    public Call<Void> sendSpans(List<byte[]> encodedSpans) {
        long now = System.currentTimeMillis();
        File tmpFile = createTempFilename(now);
        if (writeToTempFile(tmpFile, encodedSpans)) {
            File outfile = createFilename(now);
            if (!tmpFile.renameTo(outfile)) {
                Log.e(SplunkRum.LOG_TAG, "Error renaming spans temp file");
            }
        }
        return Call.create(null);
    }

    private File createFilename(long now) {
        return new File(path.toString() + File.separator + now + ".spans");
    }

    private File createTempFilename(long now) {
        return new File(createFilename(now) + ".tmp");
    }

    private boolean writeToTempFile(File tmpFile, List<byte[]> encodedSpans) {
        try {
            fileUtils.writeFileContents(tmpFile, encodedSpans);
        } catch (IOException e) {
            Log.e(SplunkRum.LOG_TAG, "Error buffering span data to disk", e);
            return false;
        }
        return true;
    }
}
