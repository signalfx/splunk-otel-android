package com.splunk.rum;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;

public class ExplicitVisibleScreenNameTracker extends VisibleScreenTracker {
    private final AtomicReference<String> lastScreenName = new AtomicReference<>();
    private final AtomicReference<String> previouslyLastScreenName = new AtomicReference<>();

    public void setExplicitScreenName(String screenName) {
        this.previouslyLastScreenName.set(this.lastScreenName.get());
        this.lastScreenName.set(screenName);
    }

    @Nullable
    @Override
    public String getPreviouslyVisibleScreen() {
        String screenName = previouslyLastScreenName.get();
        if (screenName != null) {
            return screenName;
        }

        return super.getPreviouslyVisibleScreen();
    }

    @Override
    public String getCurrentlyVisibleScreen() {
        String screenName = lastScreenName.get();
        if (screenName != null) {
            return screenName;
        }

        return super.getCurrentlyVisibleScreen();
    }

}
