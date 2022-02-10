package com.splunk.rum;

import static androidx.core.app.FrameMetricsAggregator.DRAW_DURATION;
import static androidx.core.app.FrameMetricsAggregator.DRAW_INDEX;
import static com.splunk.rum.SplunkRum.LOG_TAG;

import android.app.Activity;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.core.app.FrameMetricsAggregator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

public class SlowRenderingDetector {

    private final FrameMetricsAggregator frameMetrics = new FrameMetricsAggregator(DRAW_DURATION);
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final Set<Activity> activities = new HashSet<>();
    private final Tracer tracer;

    public SlowRenderingDetector(Tracer tracer) {
        this.tracer = tracer;
    }

    public void add(Activity activity) {
        activities.add(activity);
        frameMetrics.add(activity);
    }

    public void stop(Activity activity) {
        SparseIntArray[] arrays = frameMetrics.remove(activity);
        activities.remove(activity);
        if (arrays != null) {
            reportSlow(arrays[DRAW_INDEX]);
        }
    }

    public void start() {
        executorService.scheduleAtFixedRate(this::reportSlowRenders, 1, 1, TimeUnit.SECONDS);
    }

    private void reportSlowRenders() {
        try {
            SparseIntArray[] metrics = frameMetrics.reset();
            if (metrics != null) {
                reportSlow(metrics[DRAW_INDEX]);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception while processing frame metrics", e);
        }
        for (Activity activity : activities) {
            frameMetrics.remove(activity);
            frameMetrics.add(activity);
        }
    }

    private void reportSlow(SparseIntArray durationToCountHistogram) {
        if (durationToCountHistogram == null) {
            return;
        }
        int slowCount = 0;
        int frozenCount = 0;
        for (int i = 0; i < durationToCountHistogram.size(); i++) {
            int duration = durationToCountHistogram.keyAt(i);
            int count = durationToCountHistogram.get(duration);
            if (duration > 700) {
                Log.d(LOG_TAG, "* FROZEN RENDER DETECTED: " + duration + " ms." + count + " times");
                frozenCount += count;
            } else if (duration > 16) {
                Log.d(LOG_TAG, "* Slow render detected: " + duration + " ms. " + count + " times");
                slowCount += count;
            }
        }

        Instant now = Instant.now();
        if(slowCount > 0){
            makeSpan("slowRender", slowCount, now);
        }
        if(slowCount > 0){
            makeSpan("frozenRender", frozenCount, now);
        }
    }

    private void makeSpan(String name, int slowCount, Instant now) {
        Span span = tracer
                .spanBuilder(name)
                .setAttribute("count", slowCount)
                .setStartTimestamp(now)
                .startSpan();
        span.end(now);
    }
}
