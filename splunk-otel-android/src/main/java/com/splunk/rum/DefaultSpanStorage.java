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
    public File provideSpansDirectory() {
        File spansPath = fileUtils.getSpansDirectory(rootDir);
        if (spansPath.exists() || spansPath.mkdirs()) {
            return spansPath;
        }

        Log.e(
                SplunkRum.LOG_TAG,
                "Error creating path " + spansPath + " for span buffer, defaulting to parent");
        return rootDir;
    }

    @Override
    public Stream<File> getAllSpanFiles() {
        return fileUtils.listSpanFiles(provideSpansDirectory());
    }

    @Override
    public long getTotalFileSizeInBytes() {
        return fileUtils.getTotalFileSizeInBytes(provideSpansDirectory());
    }

    @Override
    public Stream<File> getPendingFiles() {
        return fileUtils.listSpanFiles(provideSpansDirectory());
    }
}
