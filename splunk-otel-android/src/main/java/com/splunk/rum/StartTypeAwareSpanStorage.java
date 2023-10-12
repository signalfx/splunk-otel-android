/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.util.Log;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import java.io.File;
import java.util.UUID;
import java.util.stream.Stream;

/***
 * Store span files to /span/background/@uniqueId/ for spans created when the app started in the background.
 * If the app is brought to foreground and the same session ID still in use, the background spans are moved to /span for eventual sending.
 * If the app still in the background until process-kill, the background span files will eventually be deleted by the @DeviceSpanStorageLimiter.
 */
public class StartTypeAwareSpanStorage implements SpanStorage {

    private final VisibleScreenTracker visibleScreenTracker;
    private final FileUtils fileUtils;
    private final String uniqueId;
    private final File rootDir;
    private final File spanDir;

    static StartTypeAwareSpanStorage create(VisibleScreenTracker visibleScreenTracker, FileUtils fileUtils, File rootDir){
        StartTypeAwareSpanStorage startTypeAwareSpanStorage = new StartTypeAwareSpanStorage(visibleScreenTracker, fileUtils, rootDir);
        startTypeAwareSpanStorage.cleanupUnsentBackgroundSpans();
        return startTypeAwareSpanStorage;
    }

    private StartTypeAwareSpanStorage(
            VisibleScreenTracker visibleScreenTracker, FileUtils fileUtils, File rootDir) {
        this.visibleScreenTracker = visibleScreenTracker;
        this.fileUtils = fileUtils;
        this.rootDir = rootDir;
        this.spanDir = fileUtils.getSpansDirectory(rootDir);
        this.uniqueId = UUID.randomUUID().toString();
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
        if (isAppForeground()) {
            moveBackgroundSpanToPendingSpan();
        }
        return fileUtils.listSpanFiles(spanDir);
    }

    private boolean isAppForeground() {
        return (visibleScreenTracker.getCurrentlyVisibleScreen() != null
                        && !visibleScreenTracker.getCurrentlyVisibleScreen().equals("unknown"))
                || visibleScreenTracker.getPreviouslyVisibleScreen() != null;
    }

    private void moveBackgroundSpanToPendingSpan() {
        fileUtils
                .listSpanFiles(getCurrentSessionBackgroundFile())
                .forEach(
                        file -> {
                            File destinationFile = new File(spanDir, file.getName());
                            boolean isMoved = file.renameTo(destinationFile);
                            Log.d(
                                    LOG_TAG,
                                    "Moved background span "
                                            + file.getPath()
                                            + " success ? "
                                            + isMoved
                                            + " for eventual send");
                        });
        cleanupUnsentBackgroundSpans();
    }

    private File getCurrentSessionBackgroundFile() {
        return new File(spanDir, "background/" + uniqueId);
    }

    @Override
    public File provideSpanFile() {
        return ensureDirExist(getSpanFile());
    }

    private File getSpanFile() {
        if (!isAppForeground()) {
            Log.d(LOG_TAG, "Creating background span " + uniqueId);
            return getCurrentSessionBackgroundFile();
        }
        Log.d(LOG_TAG, "Creating foreground span " + uniqueId);
        return spanDir;
    }

    private File ensureDirExist(File pathToReturn) {
        if (pathToReturn.exists() || pathToReturn.mkdirs()) {
            return pathToReturn;
        }
        Log.e(
                SplunkRum.LOG_TAG,
                "Error creating path " + pathToReturn + " for span buffer, defaulting to parent");
        return rootDir;
    }

    private void cleanupUnsentBackgroundSpans() {
        fileUtils
                .listDirectories(new File(spanDir, "background/"))
                .filter(
                        dir -> {
                            String path = dir.getPath();
                            String pathId = path.substring(path.lastIndexOf("/") + 1);
                            return !pathId.equals(uniqueId);
                        })
                .forEach(
                        dir -> {
                            Log.d(SplunkRum.LOG_TAG, "Cleaning up " + dir.getPath());
                            fileUtils.listFilesRecursively(dir).forEach(fileUtils::safeDelete);
                            fileUtils.safeDelete(dir);
                        });
    }
}
