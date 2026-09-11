#compile-time annotation
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations

# OpenTelemetry OTLP exporter references these Jackson classes that are optional on Android.
# Splunk export uses protobuf marshalers; these are not required.
-dontwarn com.fasterxml.jackson.core.JsonFactory
-dontwarn com.fasterxml.jackson.core.JsonGenerator

# OpenTelemetry OTLP exporter bundles service-provider implementations for optional
# auto-configuration APIs. Splunk RUM configures its exporters directly.
-dontwarn io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener
-dontwarn io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider
-dontwarn io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider
-dontwarn io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider
-dontwarn io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider
