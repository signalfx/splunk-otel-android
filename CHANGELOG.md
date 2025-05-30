# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### Unreleased

### Version 1.10.0 - 2025-05-22

This is a regular maintenance release

* Fixed duplicate span bug in Crash and ANR Reporting
* Updated some attribute naming around build identifiers for stacktrace symbolication

### Version 1.9.0 - 2025-02-03

This is a regular maintenance release

* Marked APIs, classes, and methods that will be deprecated down the line with the deprecation annotation

### Version 1.8.1 - 2024-12-06

Reducing the version of androidx libraries that enforce API level 35.

### Version 1.8.0 - 2024-12-04

This is a regular maintenance release.

This version depends on these upstream versions:

* opentelemetry-android v0.4.0
* opentelemetry-instrumentation-api [v1.33.6](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.33.6)
* opentelemetry-sdk [v1.41.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.41.0)

📈 Enhancements:

* Add ability to support multiple `Server-Timing` HTTP headers ([#1077](https://github.com/signalfx/splunk-otel-android/pull/1077))
* Add unique build identifiers to ANRs, crashes, and slow renders. Note: This enhancement uses experimental attribute names that are subject to change in a future release. ([#1093](https://github.com/signalfx/splunk-otel-android/pull/1093))

### Version 1.7.0 - 2024-08-12

This is a regular maintenance release.

This version depends on these upstream versions:

* opentelemetry-android v0.4.0
* opentelemetry-instrumentation-api v1.33.5
* opentelemetry-sdk v1.41.0

Enhancements:

* Disable console exporter from upstream (#894)

### Version 1.6.0 - 2024-07-10

This is a regular maintenance release.

* Update to OpenTelemetry Java Instrumentation 1.33.4
* Add experimental API to allow manually setting the current screen name. ([#851](https://github.com/signalfx/splunk-otel-android/pull/851))

### Version 1.5.0 - 2024-05-07

This is a regular maintenance release. Due to incompatible changes in upstream, `opentelemetry-android`
has been held at v0.4.0. Expect `splunk-otel-android` to have an alpha of 2.x soon.

This version depends on these upstream versions:
* `opentelemetry-android` v0.4.0
* `opentelemetry-instrumentation-api` v1.33.2
* `opentelemetry-sdk` v1.37.0

Enhancements:

* Add experimental OTLP exporter support ([#788](https://github.com/signalfx/splunk-otel-android/pull/788)).
  Note: OTLP support is incompatible with disk-buffering.

### Version 1.4.0 - 2024-03-06

This regular release follows the upstream `opentelemetry-android` release.

* Upgrade upstream `opentelemetry-android` to 0.4.0.
* Add new incubating API: `SplunkRumBuilder.setHttpSenderCustomizer()` to allow customization
  of the HTTP client used for sending data to Splunk. This can be useful when devices are
  behind a proxy or API gateway (#742).
* Fix AndroidResource blending between Splunk version and upstream. (#757)

## Version 1.3.1 - 2023-12-14

The previous release was mistakenly built against an OpenTelemetry `SNAPSHOT` build.
This remedies that and compiles against the 1.32.0 non-SNAPSHOT bom.

* Compile against OpenTelemetry Java Instrumentation 1.32.0 (no SNAPSHOT).

## Version 1.3.0 - 2023-12-13

This is a standard release following the regular upstream `opentelemetry-android` release.
This version is succeeded by a patch release: Users should use 1.3.1 instead of 1.3.0.

* Update to use upstream `opentelemetry-android` 0.3.0 (#714)
* Reduce unnecessary attempts at directory deletion for disk buffered telemetry (#683)
* Change session sampling strategy to be consistent with other splunk RUM implementations (#698)
* Global attributes and screen attributes are now handled by upstream (#710)
* Network monitoring and network attributes appender now handle by upstream (#713)
* Fix broken url targets in sample app (#715)

## Version 1.2.0 - 2023-10-25

This is the first version of `splunk-otel-android` that is based on the upstream version of
`opentelemetry-android`. Please note that this requires an additional project dependency,
as [documented here](https://docs.splunk.com/observability/en/gdi/get-data-in/rum/android/install-rum-android.html#install-the-android-agent-as-a-dependency).

* Depend on upstream [opentelemetry-android](https://github.com/open-telemetry/opentelemetry-android) (#640)
* Depend on updated java semantic conventions (#658)
* Feature enhancement: The instrumentation can now be configured to buffer telemetry created when
  your application is launched in the background. This buffered telemetry is sent when the
  app is foregrounded, or dropped when a new app session is started. Thanks to @rezastallone
  for this contribution. (#648)
* Update to opentelemetry-java sdk [1.31.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.31.0)
* Update to opentelemetry-java-instrumentation [1.31.0](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.31.0)

## Version 1.1.1

* Fix build to include `.aar` artifact. Please note that this version splits out an
  additional required dependency: `com.splunk:opentelemetry-android-instrumentation`.

## Version 1.1.0

This version had a critical defect which caused the `.aar` to not be published to maven central.
Users should skip over this version and use the patch release 1.1.1 when it is available.

* `splunk.rum.version` attribute has been renamed to `rum.sdk.version` (#524)
* new API: `OpenTelemetryRumBuilder.mergeResource()` to allow merging into resource instead of replacing (#524)
* new API: `SplunkRumBuilder.disableBackgroundTaskReporting(applicationId)` - pass the `applicationId` to this
  method in order to disable instrumentation for background tasks. (#614) (#624)
* OpenTelemetry SDK updated to [1.29.0](https://github.com/open-telemetry/opentelemetry-java/releases/tag/v1.29.0)

## Version 1.0.0

Version 1.0.0 marks the initial _stable_ release of `splunk-otel-android`.

## Version 1.0.0-rc.3

Release candidate for 1.0.0.

* Updated to OpenTelemetry Java v1.21.0 (#421)
* Guard against creating empty spans files when using disk buffering (#407)
* Don't include first frame in draw duration histogram for slow renders (#400)

## Version 1.0.0-rc.2

Release candidate for 1.0.0.

* Updated to OpenTelemetry Java v1.19.0 (#383)
* Fixed a bug where sometimes crash reports were not exported (#368)
* Added runtime details (storage, memory, battery) to crash reports (#369)
* Removed deprecated classes (#372)
* Included activity name in slow and frozen render reports (#373)
* Renamed the `SplunkRumBuilder#disableNetworkMonitorEnabled()` method to
  `SplunkRumBuilder#disableNetworkMonitor()` (#377)
* Added experimental React Native support (React Native lib will be developed and released
  separately) (#381)

## Version 1.0.0-rc.1

Release candidate for 1.0.0.

* Updated to OpenTelemetry Java v1.18.0
* Introduced a `SplunkRumBuilder` class and deprecated `Config` (#342)
* Add mobile carrier info to Span attributes (name/mcc/mnc/icc) (#358)
* Improve thread safety of slow rendering detector (#361)

## Version 0.17.0

* Ignore background application starts when measuring AppStart events (#315)
* Remove the local IP address from span (#327)
* Add `StandardAttributes.APP_BUILD_TYPE` (#328)
* Updated to OpenTelemetry Java v1.17.0 (#335)
* Crash reporting enhancements: capture larger stack traces; in case multiple exceptions happen
  during a crash only treat the first one as the cause (#323)

## Version 0.16.0

* Updated to OpenTelemetry Java v1.15.0 (#303)
* Fix race condition in slow render detection (#304)

## Version 0.15.0

* Updated to OpenTelemetry Java v1.14.0 (#287)

## Version 0.14.0

* Disk caching exporter now retries sending files (#260)
* Add ability to customize `screen.name` attribute with `@RumScreenName` annotation (#261)
* Add ability to limit storage usage for buffered telemetry (#272)
* Add method to check if RUM library has been initialized (#273)
* Add option to sample traces based on session ID (#273)

## Version 0.13.0

* Update RUM property to support GDI spec 1.2 (#198)
* Add `exception.type` and `exception.message` for crash report spans (#200)
* Initial support for Volley HurlStack instrumentation (#209)
* Support for detecting slow and frozen renders, enabled by default (#236)
* Sample app updated to support slow renders (#236)
* Updated to OpenTelemetry Java v1.12.0 (#254)
* Add experimental support of buffering telemetry through storage (#251)
* Consistency improvements to public configuration API (#255)
* Add session timeout after a period of inactivity (#226)
* Numerous dependency upgrades

## Version 0.12.0

* BUGFIX: Initialization events now share the same clock instance.
* The `beaconEndpoint` configuration setting always overrides the `realm` setting.

## Version 0.11.0

* BUGFIX: Fixed another issue that could service if the `ConnectivityManager` threw an exception when queried.
  See the corresponding Android bug: https://issuetracker.google.com/issues/175055271
* ANR spans are now properly marked as ERROR spans.
* Library now targets SDK version 31 (minimum version is still 21)
* The opentelemetry-okhttp-3.0 instrumentation has been updated to version 1.6.2.

## Version 0.10.0

* BUGFIX: Fixed a bug that could crash the application if Android's `ConnectivityManager` threw an
  exception when queried. See the corresponding Android bug: https://issuetracker.google.com/issues/175055271
* Updated OpenTelemetry okhttp instrumentation to v1.6.0.
* Capture attributes related to OkHttp Exceptions in the http client instrumentation.

## Version 0.9.0

* A span is now created when the SessionId changes to link the new session to the old one. The exact
  details of this span will probably change in the future.
* The library has been updated to use OpenTelemetry Java v1.6.0.
* All span string-valued attributes will now be truncated to 2048 characters if they exceed that
  limit.

## Version 0.8.0

* Fixed a `NullPointerException` that sometimes happened during the Network monitor initialization.
* The Zipkin exporter is now lazily initialized in a background thread. This change should greatly speed up the library startup.

## Version 0.7.0

* OpenTelemetry okhttp instrumentation has been updated to version 1.5.3-alpha.
* For okhttp, SplunkRum now exposes a wrapper for your `OkHttpClient` which implements the `Call.Factory`
  interface. This `Call.Factory` will properly manage context propagation with asynchronous http calls.
* The okhttp Interceptor provided by SplunkRum has been deprecated. Please use the `Call.Factory` from now on.
  The `createOkHttpRumInterceptor()` method will be removed in a future release.
* A new class (`com.splunk.rum.StandardAttributes`) has been introduced to provide `AttributeKey`s for
  standard RUM span attributes. Currently this class exposes the `APP_VERSION` attribute.
* The ANR detector and Network monitor will no longer operate when the app has been put in the background.
* A new API on the `Config.Builder` allows redacting of spans or redacting/replacing Span attributes. See
  the new `filterSpans(Consumer<SpanFilterBuilder>)` method and the corresponding `com.splunk.rum.SpanFilterBuilder` class
  for details.
* The `os.type` Span attribute has been changed to 'linux' and `os.name` attribute is now 'Android'.

## Version 0.6.0

* Adds proguard consumer information to assist with proguarded release builds.

## Version 0.5.0

* The initial cold `AppStart` span now starts with the library initialization and continues until the first Activity has been restored.
* Span names now have their capitalization preserved, rather than being lower-cased everywhere.

## Version 0.4.0

* All methods deprecated in v0.3.0 have been removed.
* The span names generated for Activity/Fragment lifecycle events no longer include the
  Activity/Fragment name as a prefix. There is still an attribute which tracks the name.

## Version 0.3.0

* The `com.splunk.rum.Config.Builder` class has been updated.
  * The `beaconUrl(String)` method has been deprecated and replaced with `beaconEndpoint(String)`.
  * A new `realm(String)` method has been added for easier beacon endpoint configuration.
  * The `rumAuthToken(String)` method has been deprecated and replaced with `rumAccessToken(String)`.
  * A new `deploymentEnvironment(String)` method has been added as a helper to set your deployment
    environment value.
* The method for recording exceptions has changed slightly:
  * The method that took a `String name` parameter has been deprecated.
  * New methods have been added that use the exception class name as the name of the Span.
* The `last.screen.name` attribute will only be recorded during detected screen transitions.
* New methods have been added to the `SplunkRum` API to allow updating the "global" attributes that
  are added to every span and event.

## Version 0.2.0

* Instrumentation has been updated to use OpenTelemetry v1.4.1
* ANRs are now detected by the Instrumentation and will be reported as "ANR" spans.
* A new API has been added to track timed RUM "workflows" as OpenTelemetry Span instances.
* The values reported for network types have been updated to match OpenTelemetry semantic
  conventions.
* The SplunkRum class has had a method added to return a no-op implementation of the SplunkRum
  capabilities.
* The SplunkRum initialization span now includes an attribute describing the features that have been
  configured.
* The instrumentation now tracks 3 types of AppStart spans: cold, hot and warm. Note that "hot"
  starts are not tracked for multi-Activity apps, only single-Activity.

## Version 0.1.0

This is the first official beta release of the project.
