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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.jupiter.api.Assertions.*;

import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Test;

class GlobalAttributesSupplierTest {

    @Test
    void update() {
        Attributes initial = Attributes.of(stringKey("foo"), "bar", stringKey("bar"), "baz");
        GlobalAttributesSupplier testClass = new GlobalAttributesSupplier(initial);
        testClass.update(
                builder -> {
                    builder.put("jimbo", "hutch");
                    builder.remove(stringKey("bar"));
                });
        Attributes result = testClass.get();
        assertEquals("bar", result.get(stringKey("foo")));
        assertEquals("hutch", result.get(stringKey("jimbo")));
        assertNull(result.get(stringKey("bar")));
    }
}
