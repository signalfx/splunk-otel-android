# Crash, ANR & Symbolication — Splunk Android RUM SDK

> **Load when:** User wants to tune crash/ANR defaults, configure the mapping file upload plugin, or understand crash reporting behavior.
>
> **Do not load when:** User is doing baseline setup only and is satisfied with default crash/ANR behavior (both are on by default).
>
> **Source files to verify:**
> - `integration/crash/src/` — Crash module
> - `integration/anr/src/` — ANR module
> - `instrumentation/buildtime/mapping-file/plugin/src/main/kotlin/com/splunk/rum/mappingfile/plugin/SplunkRumExtension.kt` — mapping plugin config

---

## Crash Reporting

### Default Behavior

The crash module installs automatically via ContentProvider. It captures:
- Uncaught Java/Kotlin exceptions
- Stack traces with class, method, and line information
- Exception type and message

No configuration needed for default behavior.

### Configuration

To disable crash reporting (rare):
```kotlin
CrashModuleConfiguration(isEnabled = false)
```

Pass to `SplunkRum.install()`:
```kotlin
SplunkRum.install(this, agentConfig,
    CrashModuleConfiguration(isEnabled = false)  // only if you need to disable
)
```

### Handled Exceptions

For exceptions your app catches and handles gracefully, use `CustomTracking.trackException()`:
```kotlin
try {
    riskyOperation()
} catch (e: IOException) {
    SplunkRum.instance.customTracking.trackException(e)
    showFallbackUI()
}
```

See `skill-references/custom-events.md` for the full `CustomTracking` API.

---

## ANR Detection

### Default Behavior

The ANR module detects when the main thread is blocked for an extended period. It uses
the OpenTelemetry Android ANR instrumentation internally.

No configuration needed for default behavior.

### Configuration

To disable ANR detection:
```kotlin
AnrModuleConfiguration(isEnabled = false)
```

### What Gets Captured

- ANR event with duration
- Main thread stack trace at the time of the ANR
- Splunk-specific attributes for correlation

### Notes

- ANR detection may fire in debug mode when a debugger is attached (the debugger pauses
  threads, triggering the ANR watchdog). This is expected behavior.
- On API 30+, `ApplicationExitInfo` provides additional ANR data from the system.

---

## Span Attributes

Both crash and ANR spans include standard Splunk RUM attributes:
- `component` — identifies the span source (`error` for crashes)
- Session ID, app name, deployment environment
- Device and OS information from global attributes

---

## Interaction with `spanInterceptor`

The `spanInterceptor` in `AgentConfiguration` can filter or modify crash/ANR spans:

```kotlin
AgentConfiguration(
    // ...
    spanInterceptor = { spanData ->
        val mutable = spanData.toMutableSpanData()
        // Redact sensitive data from crash attributes if needed
        mutable
        // Return null to drop the span (not recommended for crashes)
    }
)
```

> Be cautious about dropping crash spans via the interceptor. Crashes are critical
> observability data and should generally always be reported.

---

## Mapping File Upload (Symbolication)

The mapping file plugin uploads ProGuard/R8 mapping files to Splunk so that obfuscated crash
stack traces can be symbolicated into readable class and method names.

> **Plugin ID:** `com.splunk.rum-mapping-file-plugin`
> **Version:** Match your SDK version (e.g., `2.3.1`)

### What It Does

1. Generates a unique build ID for each build variant
2. Injects the build ID into the app's merged `AndroidManifest.xml`
3. After `assemble{Variant}`, uploads the R8/ProGuard mapping file to Splunk's API

### Apply the Plugin

**Kotlin DSL (`app/build.gradle.kts`):**
```kotlin
plugins {
    id("com.android.application")
    id("com.splunk.rum-mapping-file-plugin") version "2.3.1"
}
```

**Groovy DSL (`app/build.gradle`):**
```groovy
plugins {
    id "com.android.application"
    id "com.splunk.rum-mapping-file-plugin" version "2.3.1"
}
```

**Version catalog (`gradle/libs.versions.toml`):**
```toml
[plugins]
splunk-mapping-file = { id = "com.splunk.rum-mapping-file-plugin", version.ref = "splunk-rum" }
```

### Configure the Extension

```kotlin
splunkRum {
    enabled = true                          // default: true
    realm = "us0"                           // or use SPLUNK_REALM env var
    apiAccessToken = "your-api-token"       // or use SPLUNK_ACCESS_TOKEN env var
    failBuildOnUploadFailure = false        // default: false
}
```

### Authentication Precedence

The plugin resolves credentials in this order:

| Property | 1st: Extension | 2nd: Environment Variable |
|----------|---------------|--------------------------|
| Realm | `splunkRum.realm` | `SPLUNK_REALM` |
| Token | `splunkRum.apiAccessToken` | `SPLUNK_ACCESS_TOKEN` |

> **Never commit tokens to source.** Use environment variables in CI or inject via `gradle.properties` into the extension.

> **Note:** `apiAccessToken` for the mapping plugin is a Splunk **API access token** (for the
> Splunk Observability Cloud API), not the `rumAccessToken` used for RUM ingest. They are
> different credentials.

### CI/CD Setup

Set environment variables in your CI pipeline:

```bash
export SPLUNK_REALM=us0
export SPLUNK_ACCESS_TOKEN=your-api-access-token
```

Then the `splunkRum` extension can omit explicit values:
```kotlin
splunkRum {
    enabled = true
    failBuildOnUploadFailure = true  // fail CI if upload fails
}
```

### Registered Gradle Tasks

The plugin registers two tasks per build variant:

| Task | Purpose |
|------|---------|
| `splunkInjectBuildId{Variant}` | Injects a unique build ID into the merged manifest |
| `splunkUploadMappingFile{Variant}` | Uploads the mapping file after assembly |

Example for `release` variant:
```bash
./gradlew assembleRelease
# Tasks run automatically: splunkInjectBuildIdRelease, splunkUploadMappingFileRelease
```

### When to Use

- **Required for:** Release builds with `isMinifyEnabled = true` (R8/ProGuard)
- **Not needed for:** Debug builds (no obfuscation)
- **Recommended:** Enable in CI release pipeline; disable for local development

```kotlin
// Only enable for release builds
splunkRum {
    enabled = project.hasProperty("enableSplunkMappingUpload")
}
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Crash stack traces obfuscated | Set up the mapping file plugin (see above) |
| Upload fails silently | Set `failBuildOnUploadFailure = true` to see errors |
| "Missing realm" error | Set realm via extension, property, or `SPLUNK_REALM` env var |
| "Missing access token" | Set via extension, property, or `SPLUNK_ACCESS_TOKEN` env var |
| Mapping file not found | Ensure `isMinifyEnabled = true` in the build type |
| Plugin not found | Add to project-level build file; verify version matches SDK |
| ANR false positives in debug | Expected when debugger is attached; paused threads trigger the watchdog |
