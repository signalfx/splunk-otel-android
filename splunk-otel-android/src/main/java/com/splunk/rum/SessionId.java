package com.splunk.rum;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.internal.SystemClock;

class SessionId {
    private static final long SESSION_LIFETIME_NANOS = TimeUnit.HOURS.toNanos(4);

    private final Clock clock;
    private final AtomicReference<String> value = new AtomicReference<>();
    private volatile long createTimeNanos;

    SessionId() {
        this(SystemClock.getInstance());
    }

    //for testing
    SessionId(Clock clock) {
        this.clock = clock;
        getSessionId();
    }

    String getSessionId() {
        String currentValue = value.get();
        if (currentValue == null || sessionExpired()) {
            String newId = createNewId();
            //if this returns false, then another thread updated the value already.
            if (value.compareAndSet(currentValue, newId)) {
                createTimeNanos = clock.now();
            }
            return value.get();
        }
        return currentValue;
    }

    private boolean sessionExpired() {
        long elapsedTime = clock.now() - createTimeNanos;
        return elapsedTime >= SESSION_LIFETIME_NANOS;
    }

    private String createNewId() {
        Random random = new Random();
        return TraceId.fromLongs(random.nextLong(), random.nextLong());
    }
}
