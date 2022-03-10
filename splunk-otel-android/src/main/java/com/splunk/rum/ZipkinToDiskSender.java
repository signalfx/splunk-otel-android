package com.splunk.rum;

import android.util.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import zipkin2.Call;
import zipkin2.codec.Encoding;
import zipkin2.reporter.Sender;

public class ZipkinToDiskSender extends Sender {

    private final Path path;
    private final FileUtils fileUtils;
    private final Clock clock;

    public ZipkinToDiskSender(Path path) {
        this(path, new FileUtils(), Clock.systemDefaultZone());
    }

    // exists for testing
    ZipkinToDiskSender(Path path, FileUtils fileUtils, Clock clock) {
        this.path = path;
        this.fileUtils = fileUtils;
        this.clock = clock;
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
        return encodedSpans.stream().reduce(0, (acc, cur) -> acc + cur.length + 1, Integer::sum) ;
    }

    @Override
    public Call<Void> sendSpans(List<byte[]> encodedSpans) {
        long now = clock.millis();
        Path tmpFile = createTempFilename(now);
        if (writeToTempFile(tmpFile, encodedSpans)) {
            Path outfile = createFilename(now);
            try {
                fileUtils.moveAtomic(tmpFile, outfile);
            } catch (IOException e) {
                Log.e(SplunkRum.LOG_TAG, "Error renaming spans temp file");
            }
        }
        return Call.create(null);
    }

    private Path createFilename(long now) {
        return path.resolve(now + ".spans");
    }

    private Path createTempFilename(long now) {
        Path filename = createFilename(now);
        return filename.resolveSibling(filename.getFileName() + ".tmp");
    }

    private boolean writeToTempFile(Path tmpFile, List<byte[]> encodedSpans) {
        try {
            fileUtils.writeAsLines(tmpFile, encodedSpans);
        } catch (IOException e) {
            Log.e(SplunkRum.LOG_TAG, "Error buffering span data to disk", e);
            return false;
        }
        return true;
    }
}
