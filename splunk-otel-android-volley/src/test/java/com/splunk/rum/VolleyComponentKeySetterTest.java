package com.splunk.rum;

import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

public class VolleyComponentKeySetterTest {

    @Test
    public void component(){
        VolleyComponentKeySetter testClass = new VolleyComponentKeySetter();
        AttributesBuilder attributes = Attributes.builder();
        testClass.onStart(attributes, null, null);
        assertThat(attributes.build().get(COMPONENT_KEY)).isEqualTo("http");
    }

}