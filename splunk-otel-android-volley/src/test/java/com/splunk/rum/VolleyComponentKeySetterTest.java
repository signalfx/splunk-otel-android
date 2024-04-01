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

import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import org.junit.Test;

public class VolleyComponentKeySetterTest {

    @Test
    public void component() {
        VolleyComponentKeySetter testClass = new VolleyComponentKeySetter();
        AttributesBuilder attributes = Attributes.builder();
        testClass.onStart(attributes, null, null);
        assertThat(attributes.build().get(COMPONENT_KEY)).isEqualTo("http");
    }
}
