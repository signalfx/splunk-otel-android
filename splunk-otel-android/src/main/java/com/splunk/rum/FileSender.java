package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;
import static java.util.Collections.emptyList;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import zipkin2.Call;
import zipkin2.reporter.Sender;

class FileSender {

    private final static int DEFAULT_MAX_RETRIES = 10;

    private final Sender sender;
    private final FileUtils fileUtils;
    private final BandwidthTracker bandwidthTracker;
    private final RetryTracker retryTracker;

    private FileSender(Builder builder) {
        this.sender = builder.sender;
        this.fileUtils = builder.fileUtils;
        this.bandwidthTracker = builder.bandwidthTracker;
        this.retryTracker = builder.retryTracker;
    }

    /**
     * Reads a file on disk and attempts to send it. Updates the bandwidthTracker with
     * the bytes read, and return true if the file was sent. It will keep track of
     * how many attempts the file has had, and if it exceedes the max retries, the file
     * will be deleted.
     * @param file File to handle
     * @return true if the file content was sent successfully
     */
    boolean handleFileOnDisk(File file) {
        Log.d(LOG_TAG, "Reading file content for ingest: " + file);
        List<byte[]> encodedSpans = readFileCompletely(file);
        if (encodedSpans.isEmpty()) {
            return false;
        }

        boolean sentOk = attemptSend(file, encodedSpans);
        boolean doneWithRetries = sentOk || retryTracker.incrementAndCheckMax(file);
        if(doneWithRetries) {
            retryTracker.clear(file);
            fileUtils.safeDelete(file);
        }
        return sentOk;
    }

    private boolean attemptSend(File file, List<byte[]> encodedSpans) {
        try {
            bandwidthTracker.tick(encodedSpans);
            Call<Void> httpCall = sender.sendSpans(encodedSpans);
            httpCall.execute();
            Log.d(LOG_TAG, "File content " + file + " successfully uploaded");
            return true;
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error sending file content", e);
            return false;
        }
    }

    private List<byte[]> readFileCompletely(File file) {
        try {
            return fileUtils.readFileCompletely(file);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error reading span data from file " + file, e);
            return emptyList();
        }
    }

    static Builder builder() {
        return new Builder();
    }

    private static class RetryTracker {
        private final Map<File,Integer> attempts = new HashMap<>();
        private final int maxRetries;

        private RetryTracker(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public void clear(File file) {
            attempts.remove(file);
        }

        public boolean incrementAndCheckMax(File file) {
            Integer count = attempts.merge(file, 1, (cur, x) -> cur + 1);
            boolean exceededRetries = count >= maxRetries;
            if(exceededRetries){
                Log.w(LOG_TAG, "Dropping data in " + file + " (max retries exceeded " + maxRetries + ")");
            }
            return exceededRetries;
        }
    }

    static class Builder {

        private Sender sender;
        private FileUtils fileUtils = new FileUtils();
        private BandwidthTracker bandwidthTracker;
        public RetryTracker retryTracker;
        private int maxRetries = DEFAULT_MAX_RETRIES;

        Builder sender(Sender sender) {
            this.sender = sender;
            return this;
        }

        Builder fileUtils(FileUtils fileUtils){
            this.fileUtils = fileUtils;
            return this;
        }

        Builder maxRetries(int maxRetries){
            this.maxRetries = maxRetries;
            return this;
        }

        Builder bandwidthTracker(BandwidthTracker bandwidthTracker){
            this.bandwidthTracker = bandwidthTracker;
            return this;
        }

        FileSender build() {
            this.retryTracker = new RetryTracker(maxRetries);
            return new FileSender(this);
        }
    }

}
