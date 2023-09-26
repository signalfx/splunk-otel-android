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
import androidx.annotation.NonNull;
import io.opentelemetry.android.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.List;

class MemoryBufferingExporter implements SpanExporter {

    private final CurrentNetworkProvider currentNetworkProvider;

    private final SpanExporter delegate;

    private final BacklogProvider backlogProvider;

    MemoryBufferingExporter(CurrentNetworkProvider currentNetworkProvider, SpanExporter delegate, BacklogProvider backlogProvider) {
        this.currentNetworkProvider = currentNetworkProvider;
        this.delegate = delegate;
        this.backlogProvider = backlogProvider;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        backlogProvider.addAll(spans);
        if (!currentNetworkProvider.refreshNetworkStatus().isOnline()) {
            Log.i(
                    SplunkRum.LOG_TAG,
                    "Network offline, buffering " + spans.size() + " spans for eventual export.");
            return CompletableResultCode.ofSuccess();
        }
        List<SpanData> toExport = fillFromBacklog();
        Log.d(SplunkRum.LOG_TAG, "Sending " + toExport.size() + " spans for export");
        CompletableResultCode exportResult = delegate.export(toExport);
        exportResult.whenComplete(
                () -> {
                    if (exportResult.isSuccess()) {
                        return;
                    }
                    Log.i(
                            SplunkRum.LOG_TAG,
                            "Export failed. adding " + toExport.size() + " spans to the backlog");
                    addFailedSpansToBacklog(toExport);
                });
        return exportResult;
    }

    // todo Should we favor saving certain kinds of span if we're out of space? Or favor recency?
    private void addFailedSpansToBacklog(List<SpanData> toExport) {
        backlogProvider.addFailedSpansToBacklog(toExport);
    }

    @NonNull
    private List<SpanData> fillFromBacklog() {
        return backlogProvider.fillFromBacklog();
    }

    @Override
    public CompletableResultCode flush() {
        if (!backlogProvider.isEmpty()) {
            // note: the zipkin exporter has a no-op flush() method, so no need to call it after
            // this.
            return export(fillFromBacklog());
        }
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        backlogProvider.clear();
        return delegate.shutdown();
    }
}
