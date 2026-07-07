#compile-time annotation
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations

# OpenTelemetry OTLP exporter references Jackson classes that are optional on Android.
# Splunk export uses protobuf marshalers; jackson-core is not required at runtime.
-dontwarn com.fasterxml.jackson.core.**