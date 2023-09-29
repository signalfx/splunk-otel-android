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
 * Store span files to /span/background/@uniqueId/ for spans created when the app started in the background.
 * If the app is brought to foreground and the same session ID still in use, the background spans are moved to /span for eventual sending.
 * If the app still in the background until process-kill, the background span files will eventually be deleted by the @DeviceSpanStorageLimiter.
 */
public class StartTypeAwareSpanStorage implements SpanStorage {

    private final VisibleScreenTracker visibleScreenTracker;
    private final FileUtils fileUtils;
    private final String uniqueId = UUID.randomUUID().toString();
    private final File rootDir;
    private final File spanDir;

    public StartTypeAwareSpanStorage(VisibleScreenTracker visibleScreenTracker, FileUtils fileUtils, File rootDir) {
        this.visibleScreenTracker = visibleScreenTracker;
        this.fileUtils = fileUtils;
        this.rootDir = rootDir;
        this.spanDir = fileUtils.getSpansDirectory(rootDir);
    }

    @Override
    public long getTotalFileSizeInBytes() {
        return fileUtils.getTotalFileSizeInBytesRecursively(spanDir);
    }

    @Override
    public Stream<File> getAllSpanFiles() {
        return fileUtils.listFilesRecursively(spanDir);
    }

    @Override
    public Stream<File> getPendingFiles() {
        if (visibleScreenTracker.getPreviouslyVisibleScreen() != null){
            moveBackgroundSpanToPendingSpan();
        }
        return fileUtils.listSpanFiles(spanDir);
    }

    private void moveBackgroundSpanToPendingSpan() {
        fileUtils.listSpanFiles(getCurrentSessionBackgroundFile()).forEach(file -> {
            File destinationFile = new File(spanDir, file.getName());
            boolean isMoved = file.renameTo(destinationFile);
            Log.d(LOG_TAG, "Moved background span " + file.getPath() + " success ? " + isMoved + " for eventual send");
        });
    }

    private File getCurrentSessionBackgroundFile(){
        return new File(spanDir, "background/" + uniqueId);
    }

    @Override
    public File provideSpanFile() {
        return ensureDirExist(getSpanFile());
    }

    private File getSpanFile(){
        if (visibleScreenTracker.getPreviouslyVisibleScreen() == null){
            return getCurrentSessionBackgroundFile();
        }
        return spanDir;
    }

    private File ensureDirExist(File pathToReturn) {
        if (pathToReturn.exists() || pathToReturn.mkdirs()) {
            return pathToReturn;
        }
        Log.e(SplunkRum.LOG_TAG, "Error creating path " + pathToReturn + " for span buffer, defaulting to parent");
        return rootDir;
    }
}
