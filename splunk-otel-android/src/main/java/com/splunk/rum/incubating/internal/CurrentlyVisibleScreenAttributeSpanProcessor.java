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

package com.splunk.rum.incubating.internal;

import static io.opentelemetry.android.RumConstants.SCREEN_NAME_KEY;

import com.splunk.rum.incubating.CurrentlyVisibleScreen;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Experimental SpanProcessor that sets the screen.name attribute on all Spans. It obtains the
 * screen name from an instance of CurrentlyVisibleScreen, and exists primarily as a copy of
 * ScreenAttributesSpanProcessor from upstream, without the problems of coupling to the
 * VisibleScreenTracker.
 */
public final class CurrentlyVisibleScreenAttributeSpanProcessor implements SpanProcessor {

    private final CurrentlyVisibleScreen visibleScreen;

    public CurrentlyVisibleScreenAttributeSpanProcessor(CurrentlyVisibleScreen visibleScreen) {
        this.visibleScreen = visibleScreen;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String currentScreen = visibleScreen.get();
        span.setAttribute(SCREEN_NAME_KEY, currentScreen);
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        // nop
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
