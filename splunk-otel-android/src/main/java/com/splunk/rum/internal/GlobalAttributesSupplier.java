package com.splunk.rum.internal;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

/**
 * Class to hold and update the set of global attributes that are appended to each
 * span. This is used as the supplier to the GlobalAttributesSpanAppender. We need this
 * because SplunkRum exposes a topmost update() method, and we can't break that contract,
 * and there's no way to get a reference to the GlobalAttributesSpanAppender created by OtelRum.
 *
 * <p>
 * When global attributes are more fleshed out in upstream, this will hopefully improve or go away.
 *
 *  <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 *  at any time.
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
