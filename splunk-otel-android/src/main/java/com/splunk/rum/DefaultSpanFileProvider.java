package com.splunk.rum;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.util.stream.Stream;

public class DefaultSpanFileProvider implements SpanFileProvider {

    private final FileUtils fileUtils = new FileUtils();
    private Application application;

    public DefaultSpanFileProvider(Application application) {
        this.application = application;
    }

    @Override
    public File provideSpanPath() {
        File spansPath = FileUtils.getSpansDirectory(application);
        if (!spansPath.exists()) {
            if (!spansPath.mkdirs()) {
                Log.e(
                        SplunkRum.LOG_TAG,
                        "Error creating path "
                                + spansPath
                                + " for span buffer, defaulting to parent");
                spansPath = application.getApplicationContext().getFilesDir();
            }
        }
        return spansPath;
    }

    @Override
    public Stream<File> getAllSpanFiles() {
        return fileUtils.listSpanFiles(provideSpanPath());
    }

    @Override
    public long getTotalFileSizeInBytes() {
        return fileUtils.getTotalFileSizeInBytes(provideSpanPath());
    }

    @Override
    public Stream<File> getPendingFiles() {
        return fileUtils
                .listSpanFiles(provideSpanPath());
    }
}
