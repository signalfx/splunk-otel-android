# Splunk OpenTelemetry Instrumentation for Android

![Stable][stable-image]
[![Maven Central][maven-image]][maven-link]

## Documentation

- [Install the Splunk RUM Android Agent](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-mobile-and-web-applications-for-splunk-real-user-monitoring-rum/instrument-android-applications-for-splunk-rum/splunk-rum-android-agent-version-2.0.0-and-above/install-the-splunk-rum-android-agent)
- [Record Android Sessions](https://help.splunk.com/en/splunk-observability-cloud/monitor-end-user-experience/real-user-monitoring/replay-user-sessions/record-android-sessions)
- [Troubleshoot Android Instrumentation](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-mobile-and-web-applications-for-splunk-real-user-monitoring-rum/instrument-android-applications-for-splunk-rum/splunk-rum-android-agent-version-2.0.0-and-above/troubleshoot-android-instrumentation)

# Overview

The Splunk Android SDK provides comprehensive Real User Monitoring capabilities for Android applications.
Built on OpenTelemetry, it features a modular architecture that allows you to include only the instrumentations and features that you need.

For official documentation on the Splunk OTel Instrumentation for Android, see [Instrument Android applications for Splunk RUM](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-android-applications).

## Modules Overview

The agent is composed of several modules, each responsible for a specific type of instrumentation.

| Module                              | Summary                                                                                                                                          | Enabled by Default? |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| **ANR Detection**                   | Detects and reports Application Not Responding (ANR) events.                                                                                     | Yes                 |
| **App Startup Tracking**            | Measures cold, warm, and hot application start times.                                                                                            | Yes                 |
| **Crash Reporting**                 | Captures and reports application crashes with full stack traces.                                                                                 | Yes                 |
| **Custom Tracking**                 | Manually track custom events, errors, and workflows.                                                                                             | Yes                 |
| **Lifecycle Tracking**              | Captures Activity and Fragment lifecycle transitions                                                                                             | Yes                 |
| **Navigation Tracking**             | Detects screen transitions for Activities, Fragments, and Jetpack Compose Navigation                                                             | No (automatic tracking off by default) |
| **Network Change Detection**        | Monitors and reports network connectivity status changes.                                                                                        | Yes                 |
| **Network Request Instrumentation** | Manually track HTTP requests made via OkHttp3 or use gradle plugins to automatically instrument and track OkHttp3 and HttpURLConnection requests | Yes (for manual OkHttp3 tracking); No (for automatic tracking, which requires adding Gradle plugins)                 |
| **Session Replay**                  | Provides a visual replay of user sessions.                                                                                                       | No                  |
| **Slow & Frozen Render Detection**  | Detects and reports UI frames that are slow or frozen during rendering.                                                                          | Yes                 |
| **User Interaction Tracking**       | Automatically captures user taps, focus, and other UI interactions.                                                                              | Yes                 |
| **WebView Instrumentation**         | Links native RUM sessions with Browser RUM in WebView components.                                                                                | Yes                 |

## Getting Started

For complete setup instructions with code examples and advanced configuration options, please refer to the [official documentation](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-android-applications).

#### Requirements
* Android API Level 24 is the default minimum supported version. However, API Levels 21, 22, and 23 can also be supported by enabling the forceEnableOnLowerApi to true in the AgentConfiguration
* Android Gradle Plugin 8.6.0+
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

In your project's root `build.gradle` file, inside the `allprojects` block, add `mavenCentral()` to the list of repositories:
```
allprojects {
    repositories {
        google()
        mavenCentral()
        ...
    }
}
```

#### 4. Add SDK Dependency

Add the Splunk RUM agent library to your app module's `build.gradle` file dependencies:
```
implementation("com.splunk:splunk-otel-android:2.3.3")
```

The Splunk RUM SDK does not require the OpenTelemetry Android instrumentation artifact. Remove the following dependency if you previously added it only for this SDK:
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

To use the sample app, configure the following properties in your global `gradle.properties`:
```properties
splunkRealm=<realm>
splunkRumAccessToken=<a valid Splunk RUM access token for the realm>
```

## Set property for custom ContextStorageProvider (optional)

If your application registers a custom `ContextStorageProvider` via the
`META-INF/services` SPI, set the JVM system property
`io.opentelemetry.context.contextStorageProvider` to your provider's fully
qualified class name **before** calling `SplunkRum.install()`, for example:

```kotlin
System.setProperty(
    "io.opentelemetry.context.contextStorageProvider",
    "com.example.MyContextStorageProvider"
)
SplunkRum.install(this, agentConfiguration)
```

This ensures the SDK preserves your custom provider.

**Note:** By default, if this property is unset, `SplunkRum.install()` sets it to `"default"`.
This forces OpenTelemetry to use its built-in `ThreadLocal` context storage and avoids the
classpath/jar scanning that can cause `StrictMode.DiskReadViolation` on the main thread during
app startup. For most apps that already use the default `ThreadLocal` storage, this behavior is transparent.

The SDK sets this property only after `install()` clears its precondition checks; if the install is
a no-op, the property remains unchanged.

## ProGuard / R8

The SDK ships [consumer ProGuard rules](https://developer.android.com/studio/build/shrink-code#consumer-rules)
that are applied to your app's R8 build automatically — no setup required.

The SDK suppresses the following R8 warnings for compile-time-only `AutoValue` annotations
referenced by upstream OpenTelemetry:

```pro
-dontwarn com.google.auto.value.AutoValue
-dontwarn com.google.auto.value.AutoValue$Builder
-dontwarn com.google.auto.value.AutoValue$CopyAnnotations
```

The SDK also suppresses missing-class warnings for optional Jackson types referenced by the
upstream OTLP exporter JSON marshaler. Splunk export uses protobuf marshalers, so these classes are
not required at runtime:

```pro
-dontwarn com.fasterxml.jackson.core.JsonFactory
-dontwarn com.fasterxml.jackson.core.JsonGenerator
```

## Troubleshooting

For troubleshooting issues with the Splunk OpenTelemetry instrumentation of Android, see
[Troubleshoot Android instrumentation for Splunk Observability Cloud](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-mobile-and-web-applications-for-splunk-real-user-monitoring-rum/instrument-android-applications-for-splunk-rum/splunk-rum-android-agent-version-2.0.0-and-above/troubleshoot-android-instrumentation)
in the official documentation.

# License

The Splunk Android RUM Instrumentation is licensed under the terms of the Apache Software License
version 2.0. See [the license file](./LICENSE) for more details.

[stable-image]: https://img.shields.io/badge/status-stable-informational?style=for-the-badge
[maven-image]: https://img.shields.io/maven-central/v/com.splunk/splunk-otel-android?style=for-the-badge
[maven-link]: https://mvnrepository.com/artifact/com.splunk/splunk-otel-android/latest
