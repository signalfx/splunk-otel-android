package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;

/***
 * Store span files to /span/background/@uniqueId/ for span created when the app started in the background
 * if the app is brought to foreground and the same $sessionid still in use, the background spans are moved to /span for eventual sending
 * if the app still in background until process-kill, the background span files will eventually be deleted by @DeviceSpanStorageLimiter
 */
public class StartTypeAwareSpanStorage implements SpanStorage {

    private final Application application;
    private final VisibleScreenTracker visibleScreenTracker;
    private final FileUtils fileUtils;
    private final String uniqueId = UUID.randomUUID().toString();
    ;

    public StartTypeAwareSpanStorage(VisibleScreenTracker visibleScreenTracker, Application application, FileUtils fileUtils) {
        this.visibleScreenTracker = visibleScreenTracker;
        this.application = application;
        this.fileUtils = fileUtils;
    }

    private File getCurrentSessionBackgroundPath(){
        return new File(fileUtils.getSpansDirectory(application), "background/" + uniqueId);
    }

    @Override
    public long getTotalFileSizeInBytes() {
        return fileUtils.getTotalFileSizeInBytesRecursively(fileUtils.getSpansDirectory(application));
    }

    @Override
    public Stream<File> getAllSpanFiles() {
        return fileUtils.listFilesRecursively(fileUtils.getSpansDirectory(application));
    }

    @Override
    public Stream<File> getPendingFiles() {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() != null){
            moveBackgroundSpanToPendingSpan();
        }
        return fileUtils.listSpanFiles(fileUtils.getSpansDirectory(application));
    }

    private void moveBackgroundSpanToPendingSpan() {
        fileUtils.listSpanFiles(getCurrentSessionBackgroundPath()).collect(Collectors.toList()).forEach(file -> {
            File destinationFile = new File(fileUtils.getSpansDirectory(application), file.getName());
            boolean isMoved = file.renameTo(destinationFile);
            Log.i(LOG_TAG, "Moved background span " + file.getPath() + " success ? " + isMoved + " for eventual send");
        });
    }

    @Override
    public File provideSpanFile() {
        return ensureDirExist(getSpanPath());
    }

    private File ensureDirExist(File pathToReturn) {
        if (!pathToReturn.exists()) {
            if (!pathToReturn.mkdirs()) {
                Log.e(SplunkRum.LOG_TAG, "Error creating path " + pathToReturn + " for span buffer, defaulting to parent");
                pathToReturn = application.getApplicationContext().getFilesDir();
            }
        }
        return pathToReturn;
    }

    private File getSpanPath(){
        File spansPath = fileUtils.getSpansDirectory(application);
        if (visibleScreenTracker.getPreviouslyVisibleScreen() == null){
            spansPath = getCurrentSessionBackgroundPath();
        }
        return spansPath;
    }
}
