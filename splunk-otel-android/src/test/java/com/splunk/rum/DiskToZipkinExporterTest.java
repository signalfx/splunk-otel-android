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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.opentelemetry.android.instrumentation.network.CurrentNetwork;
import io.opentelemetry.android.instrumentation.network.CurrentNetworkProvider;
import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DiskToZipkinExporterTest {

    static final int BANDWIDTH_LIMIT = 20 * 1024;
    static final File spanFilesPath = new File("/path/to/thing");
    static final SpanStorage SPAN_STORAGE = mock(SpanStorage.class);
    private File file1 = null;
    private File file2 = null;
    private File imposter = null;

    @Mock private CurrentNetworkProvider currentNetworkProvider;
    @Mock private FileUtils fileUtils;
    @Mock private CurrentNetwork currentNetwork;
    @Mock FileSender sender;
    @Mock private BandwidthTracker bandwidthTracker;

    @BeforeEach
    void setup() throws Exception {
        Mockito.reset(SPAN_STORAGE);
        when(SPAN_STORAGE.provideSpansDirectory()).thenReturn(spanFilesPath);
        file1 = new File(SPAN_STORAGE.provideSpansDirectory() + File.separator + "file1.spans");
        file2 = new File(SPAN_STORAGE.provideSpansDirectory() + File.separator + "file2.spans");
        imposter =
                new File(SPAN_STORAGE.provideSpansDirectory() + File.separator + "someImposterFile.dll");

        when(currentNetworkProvider.refreshNetworkStatus()).thenReturn(currentNetwork);
        when(currentNetwork.isOnline()).thenReturn(true);
        Stream<File> files = Stream.of(file1, imposter, file2);
        when(SPAN_STORAGE.getPendingFiles()).thenReturn(files);
    }

    @Test
    void testHappyPathExport() {
        when(sender.handleFileOnDisk(file1)).thenReturn(true);
        when(sender.handleFileOnDisk(file2)).thenReturn(true);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();
        verify(sender).handleFileOnDisk(file1);
        verify(sender).handleFileOnDisk(file2);
        verify(bandwidthTracker, never()).tick(anyList());
    }

    @Test
    void fileFailureSkipsSubsequentFiles() {

        when(sender.handleFileOnDisk(file1)).thenReturn(false);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verify(sender).handleFileOnDisk(file1);
        verify(sender, never()).handleFileOnDisk(file2);
    }

    @Test
    void testSkipsWhenOffline() {
        Mockito.reset(SPAN_STORAGE);
        when(currentNetwork.isOnline()).thenReturn(false);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verifyNoMoreInteractions(SPAN_STORAGE);
        verifyNoMoreInteractions(sender);
    }

    @Test
    void testSkipsWhenOverBandwidth() {
        when(bandwidthTracker.totalSustainedRate()).thenReturn(BANDWIDTH_LIMIT + 1.0);

        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();

        verify(sender, never()).handleFileOnDisk(any());
    }

    @Test
    void testOtherExceptionsHandled() {
        when(SPAN_STORAGE.getPendingFiles()).thenThrow(new RuntimeException("unexpected!"));
        DiskToZipkinExporter exporter = buildExporter();

        exporter.doExportCycle();
        verify(sender, never()).handleFileOnDisk(any());
    }

    private DiskToZipkinExporter buildExporter() {
        return DiskToZipkinExporter.builder()
                .fileSender(sender)
                .bandwidthLimit(BANDWIDTH_LIMIT)
                .bandwidthTracker(bandwidthTracker)
                .spanFileProvider(SPAN_STORAGE)
                .connectionUtil(currentNetworkProvider)
                .build();
    }
}
