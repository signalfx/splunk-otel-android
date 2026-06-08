# WebView Instrumentation

## Requirements

- Android API level 24+ by default, or API level 21+ with `AgentConfiguration.forceEnableOnLowerApi = true`.
- Android `WebView`.
- Browser RUM JavaScript running inside the WebView must know how to read the injected `SplunkRumNative` object.

## Overview

WebView Instrumentation links native Android RUM sessions with Browser RUM running inside a WebView.

Detection sources:

- Manual calls to `WebViewNativeBridge.integrateWithBrowserRum(...)`.
- JavaScript interface methods exposed through `SplunkRumNative`.
- Current native session state and metadata from `SplunkRum.instance.session`.

Detection behavior:

- There is no module configuration for WebView integration.
- The bridge injects a JavaScript object named `SplunkRumNative`.
- The injected object exposes the current native session ID and serialized native session metadata.
- This module does not emit telemetry directly; it exposes native session context to Browser RUM.

## Quick Start

```kotlin
SplunkRum.instance.webViewNativeBridge.integrateWithBrowserRum(webView)
```

## API Documentation

### `WebViewNativeBridge`

Entry point for integrating native RUM with Browser RUM in a WebView.

```kotlin
WebViewNativeBridge.instance
```

- Kotlin: `WebViewNativeBridge.instance` or `SplunkRum.instance.webViewNativeBridge`.
- Java: `WebViewNativeBridge.getInstance()`.

### `SplunkRum.webViewNativeBridge`

Kotlin extension property for accessing the WebView bridge from a `SplunkRum` instance.

```kotlin
val SplunkRum.webViewNativeBridge: WebViewNativeBridge
```

### `WebViewNativeBridge.integrateWithBrowserRum(...)`

Injects the native RUM JavaScript interface into a WebView.

```kotlin
fun integrateWithBrowserRum(webView: WebView)
```

- `webView`: WebView that should expose native RUM session context to Browser RUM.

The injected JavaScript object is named `SplunkRumNative` and exposes:

- `nativeSessionId`: Current native session ID.
- `nativeSessionMetadata`: Current native session metadata serialized as JSON.

## Telemetry Data Model

This module does not emit telemetry directly. Browser RUM can use the injected native session context to correlate WebView activity with the native Android RUM session.

| Signal | Name | When emitted |
|---|---|---|
| N/A | N/A | No standalone telemetry. |

| Attribute | Type | Required | Description |
|---|---|---:|---|
| N/A | N/A | N/A | No standalone telemetry attributes. |

Resource / scope:

- Instrumentation scope name: N/A
- Span kind: N/A
- Span duration: N/A
