package io.opentelemetry.rum.internal;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

public class RumConstants {

    public static final String OTEL_RUM_LOG_TAG = "OpenTelemetryRum";

    public static final AttributeKey<String> COMPONENT_KEY = AttributeKey.stringKey("component");
    public static final AttributeKey<String> START_TYPE_KEY = stringKey("start.type");
    public static final String COMPONENT_APPSTART = "appstart";
    public static final String COMPONENT_UI = "ui";

    private RumConstants(){}

}
