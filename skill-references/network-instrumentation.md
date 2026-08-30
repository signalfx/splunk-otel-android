# Network Instrumentation — Splunk Android RUM SDK

> **Load when:** App makes HTTP requests (OkHttp, Retrofit, HttpURLConnection, Ktor) and the user wants request-level tracing.
>
> **Do not load when:** App has no HTTP networking or user explicitly declines network instrumentation.
>
> **Source files to verify:**
> - `integration/okhttp3/auto/src/` — OkHttp3 auto module
> - `integration/okhttp3/manual/src/` — OkHttp3 manual module
> - `integration/httpurlconnection/src/` — HttpURLConnection module
> - `instrumentation/buildtime/okhttp3-auto/plugin/` — OkHttp3 auto Gradle plugin
> - `instrumentation/buildtime/httpurlconnection-auto/plugin/` — HttpURLConnection auto Gradle plugin

Three complementary paths for HTTP request tracing. Choose based on the app's networking stack
and whether build-time bytecode weaving is acceptable.

---

## Path A: OkHttp3 Automatic (Recommended for OkHttp apps)

Requires **both** a runtime module and a build-time Gradle plugin.

### 1. Runtime Configuration

Pass to `SplunkRum.install()`:
```kotlin
OkHttp3AutoModuleConfiguration(
    isEnabled = true,
    capturedRequestHeaders = listOf("User-Agent", "Accept"),
    capturedResponseHeaders = listOf("Date", "Content-Type", "Content-Length")
)
```

### 2. Build-Time Plugin

Add the Gradle plugin to your build. The plugin applies ByteBuddy to weave OkHttp interceptors
into your app bytecode automatically — no source changes needed.

**Kotlin DSL (`app/build.gradle.kts`):**
```kotlin
plugins {
    id("com.android.application")
    id("com.splunk.rum-okhttp3-auto-plugin") version "2.3.1"
}
```

**Groovy DSL (`app/build.gradle`):**
```groovy
plugins {
    id "com.android.application"
    id "com.splunk.rum-okhttp3-auto-plugin" version "2.3.1"
}
```

**Version catalog (`gradle/libs.versions.toml`):**
```toml
[plugins]
splunk-okhttp3-auto = { id = "com.splunk.rum-okhttp3-auto-plugin", version.ref = "splunk-rum" }
```

Then in `app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.splunk.okhttp3.auto)
}
```

> Without the build-time plugin, the runtime module still registers but cannot weave
> interceptors automatically. HTTP spans will not appear.

---

## Path B: OkHttp3 Manual (No plugin required)

Wrap each `OkHttpClient` instance with instrumentation. Useful when build-time weaving is
not feasible or you want explicit control over which clients are instrumented.

### Configuration

```kotlin
OkHttp3ManualModuleConfiguration(
    capturedRequestHeaders = listOf("Content-Type", "Accept"),
    capturedResponseHeaders = listOf("Server", "Content-Type", "Content-Length")
)
```

### Usage

```kotlin
import com.splunk.rum.integration.okhttp3.manual.extension.okHttpManualInstrumentation

val originalClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .build()

// Wrap with Splunk instrumentation
val instrumentedClient: Call.Factory = SplunkRum.instance
    .okHttpManualInstrumentation
    .buildOkHttpCallFactory(originalClient)

// Use instrumentedClient for all requests
instrumentedClient.newCall(request).execute()
```

For Retrofit:
```kotlin
val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .callFactory(instrumentedClient)  // Use .callFactory(), not .client()
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

> `buildOkHttpCallFactory()` returns a `Call.Factory`, which is the primary interface
> used by both OkHttp and Retrofit. It's a drop-in replacement for `OkHttpClient`.

---

## Path C: HttpURLConnection Automatic

For apps that use `java.net.HttpURLConnection` (or `HttpsURLConnection`). Requires both
a runtime module and a build-time Gradle plugin.

### 1. Runtime Configuration

```kotlin
HttpURLModuleConfiguration(
    isEnabled = true,
    capturedRequestHeaders = listOf("Host", "Accept"),
    capturedResponseHeaders = listOf("Date", "Content-Type", "Content-Length")
)
```

### 2. Build-Time Plugin

**Kotlin DSL:**
```kotlin
plugins {
    id("com.android.application")
    id("com.splunk.rum-httpurlconnection-auto-plugin") version "2.3.1"
}
```

**Groovy DSL:**
```groovy
plugins {
    id "com.android.application"
    id "com.splunk.rum-httpurlconnection-auto-plugin" version "2.3.1"
}
```

**Version catalog:**
```toml
[plugins]
splunk-httpurl-auto = { id = "com.splunk.rum-httpurlconnection-auto-plugin", version.ref = "splunk-rum" }
```

---

## Choosing the Right Path

| HTTP Stack | Recommended Path | Plugin Required? |
|-----------|-----------------|-----------------|
| OkHttp (direct) | Path A (auto) or Path B (manual) | A: yes, B: no |
| Retrofit (uses OkHttp) | Path A (auto) or Path B (manual with `.callFactory()`) | A: yes, B: no |
| `HttpURLConnection` | Path C (auto) | Yes |
| Ktor (uses OkHttp engine) | Path A (auto) | Yes |
| Mixed OkHttp + HttpURLConnection | Both Path A + Path C | Yes (both plugins) |

## Captured Headers

Both auto and manual paths support capturing specific request/response headers as span attributes.
Only headers listed in the configuration are captured — no headers are captured by default.

```kotlin
// Good: capture headers useful for debugging
capturedRequestHeaders = listOf("User-Agent", "Accept", "Content-Type", "X-Request-ID")
capturedResponseHeaders = listOf("Content-Type", "Content-Length", "X-Response-Time")

// Avoid: capturing auth or sensitive headers
// capturedRequestHeaders = listOf("Authorization", "Cookie")
```

---

## Network Connectivity (Separate Module)

The **Network Monitor** module tracks connectivity changes and adds network attributes to all spans.
It is separate from HTTP request instrumentation and is enabled by default.

```kotlin
NetworkMonitorModuleConfiguration(isEnabled = true)  // default
```

Attributes added to spans: `network.connection.type`, carrier name, MCC, MNC, ICC.
