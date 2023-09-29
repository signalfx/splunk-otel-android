package com.splunk.rum;

import android.util.Log;

import java.io.File;
import java.util.stream.Stream;

public class DefaultSpanStorage implements SpanStorage {

    private final FileUtils fileUtils;
    private final File rootDir;

    public DefaultSpanStorage(FileUtils fileUtils, File rootDir) {
        this.fileUtils = fileUtils;
        this.rootDir = rootDir;
    }

    @Override
    public File provideSpanFile() {
        File spansPath = fileUtils.getSpansDirectory(rootDir);
        if (spansPath.exists() || spansPath.mkdirs()) {
            return spansPath;
        }

        Log.e(
                SplunkRum.LOG_TAG,
                "Error creating path "
                        + spansPath
                        + " for span buffer, defaulting to parent");
        return rootDir;
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
