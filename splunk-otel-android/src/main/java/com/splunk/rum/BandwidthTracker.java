package com.splunk.rum;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import kotlin.collections.ArrayDeque;

/**
 * Utility class to track how much bandwidth is being used by span data.
 * It tracks raw, uncompressed spans bytes being input to a sender and is not
 * intended to represent actual gzipped/compressed/tls bytes on the network.
 */
class BandwidthTracker {
    private final static int DATAPOINTS_TO_TRACK = 6;
    private final Clock clock;
    private final List<Long> times = new ArrayDeque<>();
    private final List<Long> sizes = new ArrayDeque<>();

    BandwidthTracker(){
        this(Clock.systemDefaultZone());
    }

    // Exists for testing
    BandwidthTracker(Clock clock) {
        this.clock = clock;
    }

    /**
     * Call this method with encoded zipkin span data to have it
     * tracked.
     */
    void tick(List<byte[]> zipkinSpanData) {
        if (times.size() > DATAPOINTS_TO_TRACK) {
            times.remove(0);
        }
        times.add(clock.millis());

        if (sizes.size() > DATAPOINTS_TO_TRACK) {
            sizes.remove(0);
        }
        long currentSize = zipkinSpanData.stream()
                .map(bytes -> bytes.length)
                .reduce(0, Integer::sum, Integer::sum);
        sizes.add(currentSize);
    }

    double totalSustainedRate() {
        if (sizes.size() < 2) return 0;

        // Don't count the first ingest payload
        double total = sizes.stream().skip(1).reduce(0L, Long::sum, Long::sum);

        double timeDelta = (times.get(times.size() - 1) - times.get(0)) / 1000.0;
        return total / timeDelta;
    }

    double latestBlockRate() {
        if (sizes.size() < 2) return 0;
        double lastBlockSize = sizes.get(sizes.size() - 1);
        double timeDelta = (times.get(times.size() - 1) - times.get(times.size() - 2)) / 1000.0;
        return lastBlockSize / timeDelta;
    }
}
