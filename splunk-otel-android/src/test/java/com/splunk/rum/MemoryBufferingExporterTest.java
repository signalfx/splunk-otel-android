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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.android.instrumentation.network.CurrentNetwork;
import io.opentelemetry.android.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MemoryBufferingExporterTest {
    private final CurrentNetworkProvider currentNetworkProvider =
            mock(CurrentNetworkProvider.class);
    private final CurrentNetwork currentNetwork = mock(CurrentNetwork.class);

    private final MemorySpanBuffer backlogProvider = mock(MemorySpanBuffer.class);

    @BeforeEach
    void setUp() {
        when(currentNetworkProvider.refreshNetworkStatus()).thenReturn(currentNetwork);
    }

    @Test
    void happyPath() {
        List<SpanData> spans = Arrays.asList(mock(SpanData.class), mock(SpanData.class));
        when(currentNetwork.isOnline()).thenReturn(true);
        when(backlogProvider.drain()).thenReturn(spans);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate, backlogProvider);

        when(delegate.export(spans)).thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode result = bufferingExporter.export(spans);
        assertTrue(result.isSuccess());
    }

    @Test
    void offlinePath() {
        when(currentNetwork.isOnline()).thenReturn(false, true);
        List<SpanData> spans = Arrays.asList(mock(SpanData.class), mock(SpanData.class));
        List<SpanData> secondBatch = new ArrayList<>(spans);
        SpanData anotherSpan = mock(SpanData.class);
        secondBatch.add(anotherSpan);
        when(backlogProvider.drain()).thenReturn(secondBatch);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate, backlogProvider);


        CompletableResultCode result = bufferingExporter.export(spans);
        assertTrue(result.isSuccess());
        verify(delegate, never()).export(any());

        when(delegate.export(secondBatch)).thenReturn(CompletableResultCode.ofSuccess());

        // send another span now that we're back online.
        result = bufferingExporter.export(secondBatch);

        assertTrue(result.isSuccess());
        verify(delegate).export(secondBatch);
    }

    @Test
    void retryPath() {
        SpanData one = mock(SpanData.class);
        SpanData two = mock(SpanData.class);
        SpanData three = mock(SpanData.class);
        List<SpanData> spans = Arrays.asList(one, two);
        List<SpanData> secondSpans = Arrays.asList(one, two, three);
        when(backlogProvider.drain()).thenReturn(spans, secondSpans);
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate, backlogProvider);

        when(delegate.export(spans)).thenReturn(CompletableResultCode.ofFailure());
        when(delegate.export(secondSpans))
                .thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode firstResult = bufferingExporter.export(spans);
        assertFalse(firstResult.isSuccess());

        CompletableResultCode secondResult =
                bufferingExporter.export(Collections.singletonList(three));
        assertTrue(secondResult.isSuccess());
    }

    @Test
    void flush_withBacklog() {
        SpanData one = mock(SpanData.class);
        SpanData two = mock(SpanData.class);
        List<SpanData> spans = Arrays.asList(one, two);
        when(backlogProvider.drain()).thenReturn(spans);
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate, backlogProvider);


        when(delegate.export(spans))
                .thenReturn(CompletableResultCode.ofFailure())
                .thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode firstResult = bufferingExporter.export(spans);
        assertFalse(firstResult.isSuccess());

        CompletableResultCode secondResult = bufferingExporter.flush();
        assertTrue(secondResult.isSuccess());
        // 2 times...once from the failure, and once from the flush with success
        verify(delegate, times(2)).export(spans);
    }

    @Test
    void flush() {
        when(backlogProvider.isEmpty()).thenReturn(true);
        when(currentNetwork.isOnline()).thenReturn(true);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate, backlogProvider);
        when(delegate.flush()).thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode secondResult = bufferingExporter.flush();
        assertTrue(secondResult.isSuccess());
        verify(delegate).flush();
    }

    @SuppressWarnings("unchecked")
    @Test
    void maxBacklog() {
        List<SpanData> firstSet = new ArrayList<>();
        for (int i = 0; i < 110; i++) {
            firstSet.add(mock(SpanData.class));
        }
        List<SpanData> secondSet = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            secondSet.add(mock(SpanData.class));
        }

        when(currentNetwork.isOnline()).thenReturn(true);
        when(backlogProvider.drain()).thenReturn(firstSet, secondSet);

        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate, new DefaultMemorySpanBuffer());

        when(delegate.export(firstSet)).thenReturn(CompletableResultCode.ofFailure());

        CompletableResultCode firstResult = bufferingExporter.export(firstSet);
        assertFalse(firstResult.isSuccess());

        ArgumentCaptor<List<SpanData>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        when(delegate.export(argumentCaptor.capture()))
                .thenReturn(CompletableResultCode.ofSuccess());

        CompletableResultCode secondResult = bufferingExporter.export(secondSet);
        assertTrue(secondResult.isSuccess());

        List<SpanData> value = argumentCaptor.getValue();
        // we keep only 100 of the first 110 that failed.
        assertEquals(120, value.size());
    }

    @Test
    void shutdown() {
        SpanExporter delegate = mock(SpanExporter.class);
        MemoryBufferingExporter bufferingExporter =
                new MemoryBufferingExporter(currentNetworkProvider, delegate, backlogProvider);

        bufferingExporter.shutdown();
        verify(delegate).shutdown();
    }
}
