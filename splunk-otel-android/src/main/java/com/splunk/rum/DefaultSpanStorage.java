package com.splunk.rum;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.util.stream.Stream;

public class DefaultSpanStorage implements SpanStorage {

    private final FileUtils fileUtils;
    private final Application application;

    public DefaultSpanStorage(Application application, FileUtils fileUtils) {
        this.fileUtils = fileUtils;
        this.application = application;
    }

    @Override
    public File provideSpanFile() {
        File spansPath = fileUtils.getSpansDirectory(application);
        if (spansPath.exists() || spansPath.mkdirs()) {
            return spansPath;
        }

        Log.e(
                SplunkRum.LOG_TAG,
                "Error creating path "
                        + spansPath
                        + " for span buffer, defaulting to parent");
        return application.getApplicationContext().getFilesDir();
    }

    @Override
    public Stream<File> getAllSpanFiles() {
        return fileUtils.listSpanFiles(provideSpanFile());
    }

    @Override
    public long getTotalFileSizeInBytes() {
        return fileUtils.getTotalFileSizeInBytes(provideSpanFile());
    }

    @Override
    public Stream<File> getPendingFiles() {
        return fileUtils
                .listSpanFiles(provideSpanFile());
    }
}
