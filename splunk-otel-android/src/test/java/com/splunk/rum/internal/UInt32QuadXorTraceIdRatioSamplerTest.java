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

import static com.splunk.rum.internal.UInt32QuadXorTraceIdRatioSampler.NEGATIVE_SAMPLING_RESULT;
import static com.splunk.rum.internal.UInt32QuadXorTraceIdRatioSampler.POSITIVE_SAMPLING_RESULT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class UInt32QuadXorTraceIdRatioSamplerTest {

    private final Context parentContext = Context.root().with(Span.getInvalid());

    @Test
    void sampleInclude() {
        Sampler sampler =
                UInt32QuadXorTraceIdRatioSampler.create(
                        0.5, () -> "4777abcd3f7777abcdefc6899bc11a3e");
        SamplingResult result =
                sampler.shouldSample(
                        parentContext,
                        null,
                        null,
                        null,
                        Attributes.empty(),
                        Collections.emptyList());
        assertEquals(POSITIVE_SAMPLING_RESULT.getDecision(), result.getDecision());
    }

    @Test
    void sampleDrop() {
        Sampler sampler =
                UInt32QuadXorTraceIdRatioSampler.create(
                        0.5, () -> "9777abcd3f7777abcdefc6899bc11a3e");
        SamplingResult result =
                sampler.shouldSample(
                        parentContext,
                        null,
                        null,
                        null,
                        Attributes.empty(),
                        Collections.emptyList());
        assertEquals(NEGATIVE_SAMPLING_RESULT.getDecision(), result.getDecision());
    }

    @Test
    void nullSessionMeansAlwaysPositive() {
        Sampler sampler = UInt32QuadXorTraceIdRatioSampler.create(0.00000001, () -> null);
        SamplingResult result =
                sampler.shouldSample(
                        parentContext,
                        null,
                        null,
                        null,
                        Attributes.empty(),
                        Collections.emptyList());
        assertEquals(POSITIVE_SAMPLING_RESULT.getDecision(), result.getDecision());
    }
}
