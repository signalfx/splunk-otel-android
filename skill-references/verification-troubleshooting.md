# Verification & Troubleshooting — Splunk Android RUM SDK

> **Load when:** After any setup or configuration change, or when the user reports issues with the integration.
>
> **Do not load when:** User is only in `plan` or `review` mode and has not yet applied changes.
>
> **Source files to verify:**
> - `integration/agent/api/src/main/kotlin/com/splunk/rum/integration/agent/api/SplunkRum.kt` — `instance`, `state`

---

## Verification Checklist

After setup, verify the integration is working:

### 1. Build Succeeds

```bash
./gradlew :app:assembleDebug
```

If the build fails, check:
- Dependency resolution (correct Maven coordinates and version)
- Plugin application order (Android plugin must be applied before Splunk plugins)
- Version conflicts with existing OTel dependencies

### 2. SDK Initializes

Enable debug logging temporarily:
```kotlin
AgentConfiguration(
    // ...
    enableDebugLogging = true
)
```

Run the app and check Logcat for SDK initialization messages. Filter by tag `SplunkRum`:
```
SplunkRum: install() - ...
```

### 3. Verify Agent Status

```kotlin
val status = SplunkRum.instance.state.status
when (status) {
    is Status.Running -> Log.d("Splunk", "SDK is running")
    is Status.NotRunning.NotInstalled -> Log.e("Splunk", "install() not called")
    is Status.NotRunning.Subprocess -> Log.d("Splunk", "Running in subprocess, skipped")
    is Status.NotRunning.SampledOut -> Log.d("Splunk", "Session sampled out")
    is Status.NotRunning.UnsupportedOsVersion -> Log.e("Splunk", "API level too low")
}
```

### 4. Test Custom Event

Send a test event to verify data reaches Splunk:
```kotlin
import com.splunk.rum.integration.customtracking.extension.customTracking

SplunkRum.instance.customTracking.trackCustomEvent("sdk_verification_test")
```

Check Splunk Observability Cloud RUM dashboard for the event.

### 5. Test Crash Capture

In a debug build, trigger a test crash:
```kotlin
// In a button click handler or test screen
throw RuntimeException("Splunk RUM crash test")
```

Relaunch the app — the crash report is sent on next launch. Check Splunk for the crash event.

### 6. Verify Session Replay (if enabled)

```kotlin
val replayStatus = SplunkRum.instance.sessionReplay.state.status
Log.d("Splunk", "Session replay status: $replayStatus")
```

### 7. Verify Network Instrumentation

Make an HTTP request and check Splunk for the corresponding span:
```kotlin
// OkHttp
val response = okHttpClient.newCall(
    Request.Builder().url("https://httpbin.org/get").build()
).execute()
response.close()
```

---

## Common Issues

### SDK Not Initialized

**Symptoms:** `SplunkRum.instance.state.status` is `NotInstalled`; no data in Splunk.

**Causes:**
1. `SplunkRum.install()` not called — add it to `Application.onCreate()`
2. Application class not registered — add `android:name=".YourApp"` to `AndroidManifest.xml`
3. Called from wrong process — use `instrumentedProcessName` to restrict to main process

### No Data in Splunk Dashboard

**Check in order:**
1. `enableDebugLogging = true` — look for errors in Logcat
2. Verify `realm` and `rumAccessToken` are correct and non-empty
3. Check network connectivity from the device/emulator
4. Verify the deployment environment filter in Splunk dashboard matches your config
5. Wait 1-2 minutes — there can be ingest delay

### Build Failure: Plugin Not Found

```
Plugin [id: 'com.splunk.rum-okhttp3-auto-plugin'] was not found
```

**Fix:** Ensure the plugin is available in your plugin repositories. Add to `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
```

### Build Failure: Dependency Conflict

```
Duplicate class io.opentelemetry.* found in modules ...
```

**Cause:** The app has a separate OpenTelemetry dependency that conflicts with the one
bundled in the Splunk SDK.

**Fix:** Remove the separate OTel dependency. Splunk RUM bundles its own OTel SDK and
exposes it via `SplunkRum.instance.openTelemetry`.

If you need a specific OTel version, use Gradle's dependency resolution strategy:
```kotlin
configurations.all {
    resolutionStrategy {
        force("io.opentelemetry:opentelemetry-api:1.x.x")
    }
}
```

### Navigation/Screen Names Missing

**Causes:**
1. `NavigationModuleConfiguration(isAutomatedTrackingEnabled = false)` (default) — set to `true`
2. Compose Navigation: `registerNavController()` not called
3. Custom navigation framework — use manual `navigation.track(screenName)` calls

### Session Replay Not Recording

**Check in order:**
1. `SessionReplayModuleConfiguration(isEnabled = true)` passed to `install()`
2. `sessionReplay.start()` called after `install()`
3. `samplingRate > 0` — at `0.0`, no sessions are eligible
4. Check `sessionReplay.state.status` for the specific `NotRecording` cause

### OkHttp Spans Not Appearing

**For auto instrumentation:**
1. Verify `com.splunk.rum-okhttp3-auto-plugin` is applied in `app/build.gradle.kts`
2. Verify `OkHttp3AutoModuleConfiguration(isEnabled = true)` passed to `install()`

**For manual instrumentation:**
1. Verify the client is wrapped: `okHttpManualInstrumentation.buildOkHttpCallFactory(client)`
2. Verify you're using the wrapped `Call.Factory`, not the original `OkHttpClient`

### Obfuscated Stack Traces

Crash reports from release builds show obfuscated class/method names.

**Fix:** Set up the mapping file plugin. See `skill-references/crash-and-symbolication.md`

### `deploymentEnvironment` Error

```
IllegalArgumentException: deploymentEnvironment cannot be an empty string
```

**Fix:** Provide a non-blank value: `"dev"`, `"staging"`, `"prod"`, or `BuildConfig.BUILD_TYPE`.

### High Battery/CPU Usage

1. Check `SlowRenderingModuleConfiguration.interval` — values below `Duration.ofMillis(100)` are aggressive
2. Lower `SessionReplayModuleConfiguration.samplingRate` in production
3. Consider `deferredUntilForeground = true` for apps with heavy background work

### Competing Observability SDK

If the app uses Sentry, Datadog, New Relic, or Firebase Crashlytics alongside Splunk RUM:
- Both SDKs may install uncaught exception handlers — only one will capture the final crash
- Duplicate crash reporting is possible
- OTel SDK conflicts may occur if the other SDK also uses OpenTelemetry

**Recommendation:** Use one observability SDK per signal type. If migrating, ensure the old
SDK is fully removed before validating the new one.

---

## Debug Logging

Enable verbose SDK logging during development:
```kotlin
AgentConfiguration(
    // ...
    enableDebugLogging = true  // NEVER in production
)
```

Filter Logcat by these tags:
- `SplunkRum` — main SDK lifecycle
- `SessionReplay` — session replay status
- `Navigation` — screen tracking events
- OpenTelemetry tags for span export status

> **Disable debug logging before releasing.** It is verbose and may expose internal SDK
> details in production logs.
