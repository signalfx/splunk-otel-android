---

<p align="center">
  <strong>
    <a href="#getting-started">Getting Started</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="CONTRIBUTING.md">Getting Involved</a>
    &nbsp;&nbsp;&bull;&nbsp;&nbsp;
    <a href="SECURITY.md">Security</a>
  </strong>
</p>

![Stable][stable-image]
[![OpenTelemetry Instrumentation for Java Version][otel-image]][otel-link]
[![OpenTelemetry Instrumentation for Android Version][android-image]][android-link]
[![Splunk GDI specification][gdi-image]][gdi-link]
[![Maven Central][mave-image]][maven-link]
[![Build Status][build-image]][build-link]

---

# Splunk OpenTelemetry Instrumentation for Android

For official documentation on the Splunk OTel Instrumentation for Android, see [Instrument Android applications for Splunk RUM](https://docs.splunk.com/observability/en/gdi/get-data-in/rum/android/get-android-data-in.html).

## Features

* Crash reporting
* ANR detection
* Network change detection
* Full Android Activity and Fragment lifecycle monitoring
* Access to the OpenTelemetry APIs for manual instrumentation
* SplunkRum APIs for creating custom RUM events and reporting exceptions
* Access to an OkHttp3 Call.Factory implementation for monitoring http client requests
* APIs to redact any span from export, or change span attributes before export
* Slow / frozen render detection
* Offline buffering of telemetry via storage

## Sample Application

This repository includes a sample application that demonstrates some features of the Android RUM agent.

To build and run the sample application, configure a `local.properties` file in the root of the project. The project requires the following properties:

```properties
rum.realm=<realm>
rum.access.token=<a valid Splunk RUM access token for the realm>
```

## Troubleshooting

For troubleshooting issues with the Splunk OpenTelemetry instrumentation of Android, see
[Troubleshoot Android instrumentation for Splunk Observability Cloud](https://docs.splunk.com/observability/en/gdi/get-data-in/rum/android/troubleshooting.html)
in the official documentation.

# License

The Splunk Android RUM Instrumentation is licensed under the terms of the Apache Software License
version 2.0. See [the license file](./LICENSE) for more details.

[stable-image]: https://img.shields.io/badge/status-stable-informational?style=for-the-badge
[otel-image]: https://img.shields.io/badge/otel-1.33.5-blueviolet?style=for-the-badge
[otel-link]: https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.33.5
[android-image]: https://img.shields.io/github/v/release/signalfx/splunk-otel-android?include_prereleases&style=for-the-badge
[android-link]: https://github.com/signalfx/splunk-otel-android/releases
[gdi-image]: https://img.shields.io/badge/GDI-1.4.0-blueviolet?style=for-the-badge
[gdi-link]: https://github.com/signalfx/gdi-specification/releases/tag/v1.4.0
[maven-image]: https://img.shields.io/maven-central/v/com.splunk/splunk-otel-android?style=for-the-badge
[maven-link]: https://mvnrepository.com/artifact/com.splunk/splunk-otel-android/latest
[build-image]: https://img.shields.io/github/actions/workflow/status/signalfx/splunk-otel-android/main.yaml?branch=main&style=for-the-badge
[build-link]: https://github.com/signalfx/splunk-otel-android/actions/workflows/main.yaml