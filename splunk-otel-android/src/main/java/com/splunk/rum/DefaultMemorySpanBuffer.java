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

import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

public class DefaultMemorySpanBuffer implements MemorySpanBuffer {

    // note: no need to make this queue thread-safe since it will only ever be called from the
    // BatchSpanProcessor worker thread.
    private final Queue<SpanData> backlog = new ArrayDeque<>();

    @Override
    public void addAll(Collection<SpanData> spans) {
        backlog.addAll(spans);
    }

    @Override
    public void addFailedSpansToBacklog(SpanData spanData) {
        backlog.add(spanData);
    }

    @Override
    public List<SpanData> drain() {
        List<SpanData> retries = new ArrayList<>(backlog);
        backlog.clear();
        return retries;
    }

    @Override
    public boolean isEmpty() {
        return backlog.isEmpty();
    }

    @Override
    public void clear() {
        backlog.clear();
    }

    @Override
    public int size() {
        return backlog.size();
    }
}
