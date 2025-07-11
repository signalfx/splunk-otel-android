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
[![Maven Central][maven-image]][maven-link]
[![Build Status][build-image]][build-link]

---

# Splunk Unified Android SDK

The Splunk Unified Android SDK provides comprehensive Real User Monitoring capabilities for Android applications.
Built on OpenTelemetry, it features a modular architecture that allows you to include only the instrumentations and features that you need.

For official documentation on the Splunk OTel Instrumentation for Android, see [Instrument Android applications for Splunk RUM](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-android-applications).

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
* Automatic network request instrumentation via Gradle plugins for OkHttp3 and HttpURLConnection
* Session Replay
* User interaction tracking
* WebView integration with Browser RUM

## Getting Started

For complete setup instructions with code examples and advanced configuration options, please refer to the [official documentation](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-android-applications).

#### Requirements
* Android API Level 24+ (Android 7.0)
* Android Gradle Plugin 8.6.0+
* compileSdk 35
* Java 8+ compatibility with core library desugaring

#### 1. Enable Core Library Desugaring

API levels 24 to 25 require core library desugaring activated
See [Activate desugaring in your application](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-android-applications/install-the-android-rum-agent#dce84133fa87f4b1089e140d36b1fee4e__enable-desugaring)

#### 2. Specify Java 8 Compatability

In your app module's `build.gradle` file, specify Java 8 compatibility under the `android` `compileOptions` block

```
sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8
```

#### 3. Add Maven Central Repository

In your project's root `build.gradle` file, inside the `allprojects` block, add `mavenCentral()` to the list of repositories, and also an additional URL to include Session Replay support:
```
allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
          setUrl("https://sdk.smartlook.com/android/release")
        }
        ...
    }
}
```

#### 4. Add SDK Dependency

Add the Splunk RUM agent library to your app module's `build.gradle` file dependencies:
```
implementation("com.splunk:splunk-otel-android:2.0.0-alpha1")
```

**Important:** Remove the following line from your dependencies if present, as the upstream OpenTelemetry Android repo is already linked in our SDK:
```
implementation("io.opentelemetry.android:instrumentation:2.0.0")
```

#### 5. Initialize the Agent

Initialize the Splunk RUM agent in your Application class `onCreate()` method:
```
import android.app.Application
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.EndpointConfiguration
import com.splunk.rum.integration.agent.api.SplunkRum

class AppTest: Application() {

    override fun onCreate() {
        super.onCreate()

        val agentConfiguration = AgentConfiguration(
            endpoint = EndpointConfiguration(
                realm = SPLUNK_REALM,
                rumAccessToken = SPLUNK_RUM_ACCESS_TOKEN
            ),
            appName = "<your-app-name>",
            deploymentEnvironment = "<your-deployment-environment>",
            appVersion = "<your-app-version>"
        )

        val splunkRum = SplunkRum.install(this, agentConfiguration)
    }

    companion object {
        private const val SPLUNK_REALM = "<SPLUNK_REALM>"
        private const val SPLUNK_RUM_ACCESS_TOKEN = "<YOUR_SPLUNK_ACCESS_TOKEN>"
    }
}
```

#### 6. Optional: Enable Automatic Network Request Instrumentation

Add the following Gradle Plugins for automatic network request tracking:
- `com.splunk.rum-okhttp3-auto-plugin`
- `com.splunk.rum-httpurlconnection-auto-plugin`

## Sample Application

This repository includes a sample application ('app' module) that demonstrates most features of the Android RUM agent.

To use the sample app, configure the following Build Config properties in your `global gradle.properties`:
```properties
SPLUNK_REALM=<realm>
SPLUNK_RUM_ACCESS_TOKEN=<a valid Splunk RUM access token for the realm>
```

## Troubleshooting

For troubleshooting issues with the Splunk OpenTelemetry instrumentation of Android, see
[Troubleshoot Android instrumentation for Splunk Observability Cloud](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-android-applications/troubleshooting)
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
