# Installation & Initialization — Splunk Android RUM SDK

> **Load when:** Always (baseline depth). This is the starting reference for any new integration.
>
> **Do not load when:** The app already has a working `SplunkRum.install()` call and the user only wants to add a feature module.
>
> **Source files to verify:**
> - `integration/agent/api/src/main/kotlin/com/splunk/rum/integration/agent/api/SplunkRum.kt` — `install()` signature
> - `integration/agent/api/src/main/kotlin/com/splunk/rum/integration/agent/api/AgentConfiguration.kt` — constructor params
> - `integration/agent/api/src/main/kotlin/com/splunk/rum/integration/agent/api/EndpointConfiguration.kt` — endpoint options

> **SDK:** `com.splunk:splunk-otel-android:2.3.1`
> **Min API:** 24 (21–23 experimental with `forceEnableOnLowerApi = true`)
> **Requires:** Java 8 + core library desugaring

---

## Step 1: Add Dependency

### Using Version Catalog (`gradle/libs.versions.toml`)

If the project uses a version catalog, add Splunk there first:

```toml
[versions]
splunk-rum = "2.3.1"

[libraries]
splunk-otel-android = { module = "com.splunk:splunk-otel-android", version.ref = "splunk-rum" }
```

Then in `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.splunk.otel.android)
}
```

### Direct Dependency (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.splunk:splunk-otel-android:2.3.1")
}
```

### Groovy DSL

```groovy
dependencies {
    implementation "com.splunk:splunk-otel-android:2.3.1"
}
```

> Do NOT add `io.opentelemetry.android:*` artifacts separately — they are bundled in the SDK.

---

## Step 2: Configure Realm and Token

Realm and access token should **never** be hardcoded in source. Use Gradle properties injected at build time.

### Option A: Global `gradle.properties` (recommended for local dev)

Add to `~/.gradle/gradle.properties`:
```properties
splunkRealm=us0
splunkRumAccessToken=YOUR_RUM_ACCESS_TOKEN
```

In `app/build.gradle.kts`, inject as `BuildConfig` fields:
```kotlin
android {
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        val realm = project.findProperty("splunkRealm") as? String ?: ""
        val token = project.findProperty("splunkRumAccessToken") as? String ?: ""

        buildConfigField("String", "SPLUNK_REALM", "\"$realm\"")
        buildConfigField("String", "SPLUNK_RUM_ACCESS_TOKEN", "\"$token\"")
    }
}
```

Groovy equivalent:
```groovy
android {
    buildFeatures {
        buildConfig true
    }

    defaultConfig {
        def realm = project.findProperty("splunkRealm") ?: ""
        def token = project.findProperty("splunkRumAccessToken") ?: ""

        buildConfigField "String", "SPLUNK_REALM", "\"$realm\""
        buildConfigField "String", "SPLUNK_RUM_ACCESS_TOKEN", "\"$token\""
    }
}
```

### Option B: CI/CD Environment Variables

Read from environment in `build.gradle.kts`:
```kotlin
val realm = System.getenv("SPLUNK_REALM") ?: ""
val token = System.getenv("SPLUNK_RUM_ACCESS_TOKEN") ?: ""
buildConfigField("String", "SPLUNK_REALM", "\"$realm\"")
buildConfigField("String", "SPLUNK_RUM_ACCESS_TOKEN", "\"$token\"")
```

### Option C: If the App Already Has a Config Pattern

If the app already uses a `local.properties`, `.env` file, or secrets plugin, follow that pattern instead. Do not introduce a second config mechanism.

---

## Step 3: Initialize in Application Class

### Kotlin (Minimal)

```kotlin
import android.app.Application
import com.splunk.rum.integration.agent.api.AgentConfiguration
import com.splunk.rum.integration.agent.api.EndpointConfiguration
import com.splunk.rum.integration.agent.api.SplunkRum

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        SplunkRum.install(
            this,
            AgentConfiguration(
                endpoint = EndpointConfiguration(
                    realm = BuildConfig.SPLUNK_REALM,
                    rumAccessToken = BuildConfig.SPLUNK_RUM_ACCESS_TOKEN
                ),
                appName = "MyApp",
                deploymentEnvironment = "prod"
            )
        )
    }
}
```

### Kotlin (Full-Featured)

```kotlin
import android.app.Application
import com.splunk.rum.integration.agent.api.*
import com.splunk.rum.integration.agent.api.session.SessionConfiguration
import com.splunk.rum.integration.agent.api.user.UserConfiguration
import com.splunk.rum.integration.agent.api.user.UserTrackingMode
import com.splunk.rum.integration.navigation.NavigationModuleConfiguration
import com.splunk.rum.integration.slowrendering.SlowRenderingModuleConfiguration
// import other ModuleConfiguration classes as needed

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val agentConfig = AgentConfiguration(
            endpoint = EndpointConfiguration(
                realm = BuildConfig.SPLUNK_REALM,
                rumAccessToken = BuildConfig.SPLUNK_RUM_ACCESS_TOKEN
            ),
            appName = "MyApp",
            appVersion = BuildConfig.VERSION_NAME,
            deploymentEnvironment = BuildConfig.BUILD_TYPE,
            enableDebugLogging = BuildConfig.DEBUG,
            user = UserConfiguration(
                trackingMode = UserTrackingMode.ANONYMOUS_TRACKING
            ),
            session = SessionConfiguration(
                samplingRate = 1.0
            )
        )

        val moduleConfigs = arrayOf(
            NavigationModuleConfiguration(
                isEnabled = true,
                isAutomatedTrackingEnabled = true
            ),
            SlowRenderingModuleConfiguration(
                isEnabled = true
            )
            // Add other ModuleConfigurations here
        )

        SplunkRum.install(this, agentConfig, *moduleConfigs)
    }
}
```

### Java (Minimal)

```java
import android.app.Application;
import com.splunk.rum.integration.agent.api.AgentConfiguration;
import com.splunk.rum.integration.agent.api.EndpointConfiguration;
import com.splunk.rum.integration.agent.api.SplunkRum;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        SplunkRum.install(
            this,
            new AgentConfiguration(
                new EndpointConfiguration(
                    BuildConfig.SPLUNK_REALM,
                    BuildConfig.SPLUNK_RUM_ACCESS_TOKEN
                ),
                "MyApp",        // appName
                "prod",         // deploymentEnvironment
                null,           // appVersion
                false,          // enableDebugLogging
                null,           // globalAttributes
                null,           // spanInterceptor
                null,           // user
                null,           // session
                null,           // instrumentedProcessName
                false,          // deferredUntilForeground
                false           // forceEnableOnLowerApi
            )
        );
    }
}
```

### Custom Endpoint (Non-Realm)

For self-hosted or custom ingest endpoints:
```kotlin
EndpointConfiguration(trace = URL("https://your-ingest.example.com/v1/traces?auth=YOUR_TOKEN"))

// Or with separate session replay endpoint:
EndpointConfiguration(
    trace = URL("https://your-ingest.example.com/v1/traces?auth=YOUR_TOKEN"),
    sessionReplay = URL("https://your-ingest.example.com/v1/logs?auth=YOUR_TOKEN")
)
```

> Custom URL constructors extract the `?auth=TOKEN` query parameter automatically.

---

## Step 4: Register Application in AndroidManifest.xml

If you created a new Application class, register it:

```xml
<application
    android:name=".MyApp"
    ... >
```

If the manifest already has `android:name`, the Application class already exists — add `SplunkRum.install()` to its `onCreate()`.

> No additional manifest entries are needed. Integration module manifests are merged automatically via AAR dependencies.

---

## Endpoint Configuration Reference

| Constructor | Use Case |
|-------------|----------|
| `EndpointConfiguration(realm, rumAccessToken)` | Standard Splunk Cloud setup |
| `EndpointConfiguration(trace: URL)` | Custom trace endpoint (token in `?auth=` param) |
| `EndpointConfiguration(trace: URL, sessionReplay: URL)` | Separate trace + replay endpoints |

Realm-based URLs resolve to:
- Traces: `https://rum-ingest.{realm}.observability.splunkcloud.com/v1/traces`
- Session Replay: `https://rum-ingest.{realm}.observability.splunkcloud.com/v1/logs`

---

## What Gets Enabled by Default

After `SplunkRum.install()`, these modules activate automatically (via ContentProvider-based self-registration):

| Module | What It Does |
|--------|-------------|
| Crash | Captures uncaught exceptions with stack traces |
| ANR | Detects application-not-responding events |
| App Startup | Measures cold/warm/hot start timing |
| Slow Rendering | Polls for slow/frozen frames (1s default) |
| Interactions | Records tap, focus, keyboard, rage tap events |
| Network Monitor | Tracks connectivity changes, adds carrier info to spans |
| Lifecycle | Activity/Fragment lifecycle events |
| App Lifecycle | App foreground/background/created events |
| Navigation | Screen tracking (module on; auto-tracking off by default) |
| OkHttp3 Auto | Runtime hooks (needs build-time plugin for full auto) |
| OkHttp3 Manual | Manual client wrapping available |
| HttpURLConnection | Runtime hooks (needs build-time plugin for full auto) |

**Not enabled by default:** Session Replay (requires config + explicit `start()`), build-time plugins.
