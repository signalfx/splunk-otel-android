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

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Utility class to track how much bandwidth is being used by span data. It tracks raw, uncompressed
 * spans bytes being input to a sender and is not intended to represent actual
 * gzipped/compressed/tls bytes on the network.
 */
class BandwidthTracker {
    private static final int DATAPOINTS_TO_TRACK = 6;
    private final Clock clock;
    private final ArrayDeque<Long> times = new ArrayDeque<>();
    private final ArrayDeque<Long> sizes = new ArrayDeque<>();

    BandwidthTracker() {
        this(Clock.systemDefaultZone());
    }

    // Exists for testing
    BandwidthTracker(Clock clock) {
        this.clock = clock;
    }

    /** Call this method with encoded zipkin span data to have it tracked. */
    void tick(List<byte[]> zipkinSpanData) {
        if (times.size() > DATAPOINTS_TO_TRACK) {
            times.removeFirst();
        }
        times.add(clock.millis());

        if (sizes.size() > DATAPOINTS_TO_TRACK) {
            sizes.removeFirst();
        }
        long currentSize =
                zipkinSpanData.stream()
                        .map(bytes -> bytes.length)
                        .reduce(0, Integer::sum, Integer::sum);
        sizes.add(currentSize);
    }

    /**
     * Calculates the current average sustained throughput.
     *
     * @return - The currently tracked bandwidth, in bytes per second.
     */
    double totalSustainedRate() {
        if (sizes.size() < 2) return 0;

        // Don't count the first ingest payload
        double total = sizes.stream().skip(1).reduce(0L, Long::sum, Long::sum);

        double timeDeltaInSeconds = (times.getLast() - times.getFirst()) / 1000.0;
        return total / timeDeltaInSeconds;
    }
}
