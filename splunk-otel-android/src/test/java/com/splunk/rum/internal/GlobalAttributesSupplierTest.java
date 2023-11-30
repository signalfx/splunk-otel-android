package com.splunk.rum.internal;

import static org.junit.jupiter.api.Assertions.*;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;

class GlobalAttributesSupplierTest {

    @Test
    void update(){
        Attributes initial = Attributes.of(stringKey("foo"), "bar", stringKey("bar"), "baz");
        GlobalAttributesSupplier testClass = new GlobalAttributesSupplier(initial);
        testClass.update(builder -> {
            builder.put("jimbo", "hutch");
            builder.remove(stringKey("bar"));
        });
        Attributes result = testClass.get();
        assertEquals("bar", result.get(stringKey("foo")));
        assertEquals("hutch", result.get(stringKey("jimbo")));
        assertNull(result.get(stringKey("bar")));
    }

}