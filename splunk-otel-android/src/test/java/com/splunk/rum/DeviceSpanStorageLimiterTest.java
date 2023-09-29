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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceSpanStorageLimiterTest {

    private static final int MAX_STORAGE_USE_MB = 3;
    private static final long MAX_STORAGE_USE_BYTES = MAX_STORAGE_USE_MB * 1024 * 1024;
    @Mock private FileUtils fileUtils;
    @Mock private SpanStorage spanStorage;
    private DeviceSpanStorageLimiter limiter;

    @BeforeEach
    void setup() {
        limiter =
                DeviceSpanStorageLimiter.builder()
                        .fileUtils(fileUtils)
                        .fileProvider(spanStorage)
                        .maxStorageUseMb(MAX_STORAGE_USE_MB)
                        .build();
    }

    @Test
    void ensureFreeSpace_littleUsageEnoughFreeSpace() {
        File mockFile = mock(File.class);
        when(spanStorage.getTotalFileSizeInBytes()).thenReturn(10 * 1024L);
        when(spanStorage.provideSpanFile()).thenReturn(mockFile);
        when(mockFile.getFreeSpace()).thenReturn(99L); // Disk is very full
        assertFalse(limiter.ensureFreeSpace());
        verify(fileUtils, never()).safeDelete(any());
    }

    @Test
    void ensureFreeSpace_littleUsageButNotEnoughFreeSpace() {
        File mockFile = mock(File.class);
        when(spanStorage.getTotalFileSizeInBytes()).thenReturn(10 * 1024L);
        when(spanStorage.provideSpanFile()).thenReturn(mockFile);
        when(mockFile.getFreeSpace()).thenReturn(MAX_STORAGE_USE_BYTES * 99); // lots of room
        assertTrue(limiter.ensureFreeSpace());
        verify(fileUtils, never()).safeDelete(any());
    }

    @Test
    void ensureFreeSpace_underLimit() {
        File mockFile = mock(File.class);
        when(spanStorage.provideSpanFile()).thenReturn(mockFile);

        when(spanStorage.getTotalFileSizeInBytes()).thenReturn(MAX_STORAGE_USE_BYTES - 1);
        when(mockFile.getFreeSpace()).thenReturn(MAX_STORAGE_USE_BYTES + 1);
        boolean result = limiter.ensureFreeSpace();
        assertTrue(result);
        verify(fileUtils, never()).safeDelete(any());
    }

    @Test
    void ensureFreeSpace_overLimitHappyDeletion() {
        File file1 = new File("oldest");
        File file2 = new File("younger");
        File file3 = new File("newest");

        File mockFile = mock(File.class);
        when(spanStorage.provideSpanFile()).thenReturn(mockFile);
        when(spanStorage.getTotalFileSizeInBytes()).thenReturn(MAX_STORAGE_USE_BYTES + 1);
        when(fileUtils.getModificationTime(file1)).thenReturn(1000L);
        when(fileUtils.getModificationTime(file2)).thenReturn(1001L);
        when(fileUtils.getModificationTime(file3)).thenReturn(1002L);
        when(fileUtils.getFileSize(isA(File.class))).thenReturn(1L);
        when(spanStorage.getAllSpanFiles()).thenReturn(Stream.of(file3, file1, file2));
        when(mockFile.getFreeSpace()).thenReturn(MAX_STORAGE_USE_BYTES + 1);
        boolean result = limiter.ensureFreeSpace();

        verify(fileUtils).safeDelete(file1);
        verify(fileUtils).safeDelete(file2);
        verify(fileUtils, never()).safeDelete(file3);
        assertTrue(result);
    }
}
