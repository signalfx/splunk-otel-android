package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Store span files to /span/background/$sessionid/ for span created when the app started in the background
 * if the app is brought to foreground and the same $sessionid still in use, the background spans are moved to /span for eventual sending
 * if the app still in background until process-kill, the background span files will eventually be deleted by @DeviceSpanStorageLimiter
 */
public class StartTypeAwareSpanFileProvider implements SpanFileProvider {

    private final Application application;
    private final VisibleScreenTracker visibleScreenTracker;
    private final FileUtils fileUtils = new FileUtils();
    private final SessionId sessionId;

    public StartTypeAwareSpanFileProvider(VisibleScreenTracker visibleScreenTracker, Application application, SessionId sessionId) {
        this.visibleScreenTracker = visibleScreenTracker;
        this.application = application;
        this.sessionId = sessionId;
    }

    private File getCurrentSessionBackgroundPath(){
        return new File(FileUtils.getSpansDirectory(application), "background/" + sessionId);
    }

    @Override
    public long getTotalFileSizeInBytes() {
        return fileUtils.getTotalFileSizeInBytesRecursively(FileUtils.getSpansDirectory(application));
    }

    @Override
    public Stream<File> getAllSpanFiles() {
        return fileUtils.listFilesRecursively(FileUtils.getSpansDirectory(application));
    }

    @Override
    public Stream<File> getPendingFiles() {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() != null){
            moveBackgroundSpanToPendingSpan();
        }
        return fileUtils.listSpanFiles(FileUtils.getSpansDirectory(application));
    }

    private void moveBackgroundSpanToPendingSpan() {
        fileUtils.listSpanFiles(getCurrentSessionBackgroundPath()).map(file -> {
            File destinationFile = new File(FileUtils.getSpansDirectory(application), file.getName());
            file.renameTo(destinationFile);
            Log.i(LOG_TAG, "Moved background span " + file.getPath() + " for eventual sending");
            return file;
        }).collect(Collectors.toList());
    }

    @Override
    public File provideSpanPath() {
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
        File spansPath = FileUtils.getSpansDirectory(application);
        if (visibleScreenTracker.getPreviouslyVisibleScreen() == null){
            spansPath = getCurrentSessionBackgroundPath();
        }
        return spansPath;
    }
}
