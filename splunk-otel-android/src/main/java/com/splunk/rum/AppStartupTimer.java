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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

class AppStartupTimer {
    private final AtomicLong firstPossibleTimestamp = new AtomicLong(System.currentTimeMillis());
    private final AtomicReference<Span> overallAppStartSpan = new AtomicReference<>();

    Span start(Tracer tracer) {
        //guard against a double-start and just return what's already in flight.
        if (overallAppStartSpan.get() != null) {
            return overallAppStartSpan.get();
        }
        final Span appStart = tracer.spanBuilder("AppStart")
                .setStartTimestamp(firstPossibleTimestamp.get(), TimeUnit.MILLISECONDS)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART)
                .setAttribute(SplunkRum.START_TYPE_KEY, "cold")
                .startSpan();
        overallAppStartSpan.set(appStart);
        return appStart;
    }

    void end() {
        Span span = overallAppStartSpan.get();
        if (span != null) {
            span.end();
            overallAppStartSpan.set(null);
        }
    }

    Span getStartupSpan() {
        return overallAppStartSpan.get();
    }
}
