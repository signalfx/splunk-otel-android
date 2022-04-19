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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionIdRatioBasedSamplerTest {
    private static final String HIGH_ID = "00000000000000008fffffffffffffff";
    private static final String LOW_ID = "00000000000000000000000000000000";
    private static final IdGenerator idsGenerator = IdGenerator.random();

    private final String traceId = idsGenerator.generateTraceId();
    private final Context parentContext = Context.root().with(Span.getInvalid());
    private final List<LinkData> parentLinks =
            Collections.singletonList(LinkData.create(SpanContext.getInvalid()));

    @Test
    public void samplerUsesSessionId() {
        SessionId sessionId = mock(SessionId.class);
        SessionIdRatioBasedSampler sampler = new SessionIdRatioBasedSampler(0.5, sessionId);

        // Sampler drops if TraceIdRatioBasedSampler would drop this sessionId
        when(sessionId.getSessionId()).thenReturn(HIGH_ID);
        assertEquals(shouldSample(sampler), SamplingDecision.DROP);

        // Sampler accepts if TraceIdRatioBasedSampler would accept this sessionId
        when(sessionId.getSessionId()).thenReturn(LOW_ID);
        assertEquals(shouldSample(sampler), SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    public void zeroRatioDropsAll() {
        SessionId sessionId = mock(SessionId.class);
        SessionIdRatioBasedSampler sampler = new SessionIdRatioBasedSampler(0.0, sessionId);

        for (String id : Arrays.asList(HIGH_ID, LOW_ID)) {
            when(sessionId.getSessionId()).thenReturn(id);
            assertEquals(shouldSample(sampler), SamplingDecision.DROP);
        }
    }

    @Test
    public void oneRatioAcceptsAll() {
        SessionId sessionId = mock(SessionId.class);
        SessionIdRatioBasedSampler sampler = new SessionIdRatioBasedSampler(1.0, sessionId);

        for (String id : Arrays.asList(HIGH_ID, LOW_ID)) {
            when(sessionId.getSessionId()).thenReturn(id);
            assertEquals(shouldSample(sampler), SamplingDecision.RECORD_AND_SAMPLE);
        }
    }

    private SamplingDecision shouldSample(Sampler sampler) {
        return sampler.shouldSample(
                        parentContext,
                        traceId,
                        "name",
                        SpanKind.INTERNAL,
                        Attributes.empty(),
                        parentLinks)
                .getDecision();
    }
}
