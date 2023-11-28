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

package com.splunk.rum.internal;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * This class is very similar to the SessionIdRatioBasedSampler from upstream, but exists in order
 * to perform a trace id into a long calculation in a way that is more consistent with iOS and js.
 *
 * <p>This class should be considered a stop-gap measure until this problem is correctly spec'd in
 * otel.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class UInt32QuadXorTraceIdRatioSampler implements Sampler {
    static final SamplingResult POSITIVE_SAMPLING_RESULT = SamplingResult.recordAndSample();

    static final SamplingResult NEGATIVE_SAMPLING_RESULT = SamplingResult.drop();
    private final long idUpperBound;
    private final String description;
    private final Supplier<String> sessionIdSupplier;
    private final Object lock = new Object();
    private String lastSeenSessionId = "";
    private SamplingResult lastSamplingResult = NEGATIVE_SAMPLING_RESULT;

    public static Sampler create(double ratio, Supplier<String> sessionIdSupplier) {
        // Taken directly mostly from the TraceIdRatioBasedSampler in upstream, with a modification
        // to the upper bound to make it within UInt32.
        if (ratio < 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("ratio must be in range [0.0, 1.0]");
        }
        long idUpperBound;
        // Special case the limits, to avoid any possible issues with lack of precision across
        // double/long boundaries. For probability == 0.0, we use Long.MIN_VALUE as this guarantees
        // that we will never sample a trace, even in the case where the id == Long.MIN_VALUE, since
        // Math.Abs(Long.MIN_VALUE) == Long.MIN_VALUE.
        if (ratio == 0.0) {
            idUpperBound = Long.MIN_VALUE;
        } else if (ratio == 1.0) {
            idUpperBound = Long.MAX_VALUE;
        } else {
            idUpperBound = (long) (ratio * 0xFFFFFFFFL);
        }
        String description =
                String.format(
                        Locale.getDefault(), "UInt32QuadXorTraceIdRatioSampler{radio:%f}", ratio);
        return new UInt32QuadXorTraceIdRatioSampler(idUpperBound, sessionIdSupplier, description);
    }

    private UInt32QuadXorTraceIdRatioSampler(
            long idUpperBound, Supplier<String> sessionIdSupplier, String description) {
        this.idUpperBound = idUpperBound;
        this.sessionIdSupplier = sessionIdSupplier;
        this.description = description;
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        String sessionId = sessionIdSupplier.get();
        if (sessionId == null) {
            return POSITIVE_SAMPLING_RESULT; // Have to return true because we may not have a
            // session yet
        }
        synchronized (lock) {
            if (lastSeenSessionId.equals(sessionId)) {
                return lastSamplingResult;
            }
            lastSeenSessionId = sessionId;
            lastSamplingResult =
                    SessionUtils.convertToUInt32(sessionId) < idUpperBound
                            ? POSITIVE_SAMPLING_RESULT
                            : NEGATIVE_SAMPLING_RESULT;
            return lastSamplingResult;
        }
    }

    @Override
    public String getDescription() {
        return description;
    }
}
