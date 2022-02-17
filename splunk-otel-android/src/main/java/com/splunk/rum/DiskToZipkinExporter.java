package com.splunk.rum;

import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import zipkin2.reporter.Sender;

public class DiskToZipkinExporter {

    private final ScheduledExecutorService threadPool;
    private final Sender sender;
    private final File spanFilesPath;
    private final FileUtils fileUtils;

    public DiskToZipkinExporter(Builder builder) {
        this.threadPool = builder.threadPool;
        this.sender = builder.sender;
        this.spanFilesPath = builder.spanFilesPath;
        this.fileUtils = builder.fileUtils;
    }

    void startPolling(){
        threadPool.scheduleAtFixedRate(this::doExportCycle, 5, 5, TimeUnit.SECONDS);
    }

    private void doExportCycle() {
        File[] files = fileUtils.listFiles(spanFilesPath, (FileFilter) pathname ->
                pathname.isFile() && pathname.toString().endsWith(".spans"));
        for (File file : files) {
            byte[] fileContent = readFileCompletely(file);
            sender.sendSpans(Collections.singletonList(fileContent));
        }
    }

    private byte[] readFileCompletely(File file) {
        try {
            return fileUtils.readFileCompletely(file);
        } catch (IOException e) {
            Log.w(SplunkRum.LOG_TAG, "Error reading span data from file " + file, e);
            return new byte[0];
        }
    }

    void stop(){
        threadPool.shutdown();
    }

    void doExport(File inputFile){
        try {
            FileInputStream in = new FileInputStream(inputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    static Builder builder(){
        return new Builder();
    }

    static class Builder {
        private ScheduledExecutorService threadPool;
        private  Sender sender;
        private  File spanFilesPath;
        private  FileUtils fileUtils = new FileUtils();

        Builder threadPool(ScheduledExecutorService threadPool){
            this.threadPool = threadPool;
            return this;
        }

        Builder sender(Sender sender){
            this.sender = sender;
            return this;
        }

        Builder spanFilesPath(File spanFilesPath){
            this.spanFilesPath = spanFilesPath;
            return this;
        }

        Builder fileUtils(FileUtils fileUtils){
            this.fileUtils = fileUtils;
            return this;
        }

        DiskToZipkinExporter build(){
            return new DiskToZipkinExporter(this);
        }
    }

}
