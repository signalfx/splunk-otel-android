#compile-time annotation
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations

# OpenTelemetry OTLP exporter references these Jackson classes that are optional on Android.
# Splunk export uses protobuf marshalers; these are not required.
-dontwarn com.fasterxml.jackson.core.JsonFactory
-dontwarn com.fasterxml.jackson.core.JsonGenerator