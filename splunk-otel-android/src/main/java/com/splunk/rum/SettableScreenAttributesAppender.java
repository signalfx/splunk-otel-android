package com.splunk.rum;

import static io.opentelemetry.android.RumConstants.LAST_SCREEN_NAME_KEY;
import static io.opentelemetry.android.RumConstants.SCREEN_NAME_KEY;

import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class SettableScreenAttributesAppender implements SpanProcessor {

    private final VisibleScreenTracker visibleScreenTracker;
    private final AtomicReference<String> lastScreenName = new AtomicReference<>();
    private final AtomicReference<String> previouslyLastScreenName = new AtomicReference<>();

    public SettableScreenAttributesAppender(VisibleScreenTracker visibleScreenTracker) {
        this.visibleScreenTracker = visibleScreenTracker;
    }

    public void setScreenName(String screenName) {
        this.previouslyLastScreenName.set(this.lastScreenName.get());
        this.lastScreenName.set(screenName);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        setLastScreen(span);
        setPreviouslyLastScreen(span);
    }

    private void setLastScreen(ReadWriteSpan span) {
        if (span.getAttribute(SCREEN_NAME_KEY) != null) {
            return;
        }

        String screenName = this.lastScreenName.get();
        if (screenName != null) {
            span.setAttribute(SCREEN_NAME_KEY, screenName);
            return;
        }

        screenName = visibleScreenTracker.getCurrentlyVisibleScreen();
        if (screenName != null) {
            span.setAttribute(SCREEN_NAME_KEY, screenName);
        }
    }

    private void setPreviouslyLastScreen(ReadWriteSpan span) {
        if (span.getAttribute(LAST_SCREEN_NAME_KEY) != null) {
            return;
        }

        String screenName = this.previouslyLastScreenName.get();
        if (screenName != null) {
            span.setAttribute(LAST_SCREEN_NAME_KEY, screenName);
            return;
        }

        screenName = visibleScreenTracker.getPreviouslyVisibleScreen();
        if (screenName != null) {
            span.setAttribute(LAST_SCREEN_NAME_KEY, screenName);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
