package com.splunk.rum.internal;

import static com.splunk.rum.internal.UInt32QuadXorTraceIdRatioSampler.NEGATIVE_SAMPLING_RESULT;
import static com.splunk.rum.internal.UInt32QuadXorTraceIdRatioSampler.POSITIVE_SAMPLING_RESULT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

class UInt32QuadXorTraceIdRatioSamplerTest {

    private final Context parentContext = Context.root().with(Span.getInvalid());
    @Test
    void sampleInclude(){
        Sampler sampler = UInt32QuadXorTraceIdRatioSampler.create(0.5, () -> "4777abcd3f7777abcdefc6899bc11a3e");
        SamplingResult result = sampler.shouldSample(parentContext, null, null, null, Attributes.empty(),
                Collections.emptyList());
        assertEquals(POSITIVE_SAMPLING_RESULT.getDecision(), result.getDecision());
    }

    @Test
    void sampleDrop(){
        Sampler sampler = UInt32QuadXorTraceIdRatioSampler.create(0.5, () -> "9777abcd3f7777abcdefc6899bc11a3e");
        SamplingResult result = sampler.shouldSample(parentContext, null, null, null, Attributes.empty(),
                Collections.emptyList());
        assertEquals(NEGATIVE_SAMPLING_RESULT.getDecision(), result.getDecision());
    }


}