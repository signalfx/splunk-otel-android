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
import io.opentelemetry.api.common.AttributesBuilder;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Class to hold and update the set of global attributes that are appended to each span. This is
 * used as the supplier to the GlobalAttributesSpanAppender. We need this because SplunkRum exposes
 * a topmost update() method, and we can't break that contract, and there's no way to get a
 * reference to the GlobalAttributesSpanAppender created by OtelRum.
 *
 * <p>When global attributes are more fleshed out in upstream, this will hopefully improve or go
 * away.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public class GlobalAttributesSupplier implements Supplier<Attributes> {
    private Attributes attributes;

    public GlobalAttributesSupplier(Attributes globalAttributes) {
        this.attributes = globalAttributes;
    }

    @Override
    public Attributes get() {
        return attributes;
    }

    public void update(Consumer<AttributesBuilder> attributesUpdater) {
        AttributesBuilder builder = attributes.toBuilder();
        attributesUpdater.accept(builder);
        attributes = builder.build();
    }
}
