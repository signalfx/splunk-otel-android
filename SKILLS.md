---
name: splunk-android-rum-sdk
description: >-
  Full Splunk RUM SDK setup for Android. Use when asked to "add Splunk RUM to Android",
  "install Splunk Android SDK", "configure Splunk Android SDK", "record Android user sessions",
  "session replay Android", or instrument an Android app with Splunk Observability.
  Supports Kotlin and Java codebases, Groovy and Kotlin DSL builds, and version catalog projects.
disable-model-invocation: true
---

# Splunk Android RUM SDK

Opinionated wizard that scans an Android project and guides complete Splunk RUM setup — crash/ANR
reporting, screen tracking, network instrumentation, slow rendering, custom events, session replay,
and more.

## Invoke This Skill When

- User asks to "add Splunk RUM to Android" or "set up Splunk" in an Android app
- User wants crash reporting, ANR detection, session replay, network tracing, or screen tracking
- User mentions `splunk-otel-android`, `com.splunk:splunk-otel-android`, Splunk RUM, or Splunk Observability for Android
- User wants to record Android user sessions or enable session replay

> **SDK version:** `com.splunk:splunk-otel-android:2.3.1`
> Always verify against [Splunk Android RUM docs](https://help.splunk.com/en/splunk-observability-cloud/manage-data/available-data-sources/supported-integrations-in-splunk-observability-cloud/rum-instrumentation/instrument-mobile-and-web-applications-for-splunk-real-user-monitoring-rum/instrument-android-applications-for-splunk-rum/splunk-rum-android-agent-version-2.0.0-and-above/install-the-splunk-rum-android-agent).

---

## Modes

The skill operates in one of four modes. Default to **plan** unless the user explicitly requests otherwise.

| Mode | What the agent does | Writes code? |
|------|-------------------|-------------|
| `plan` | Detect project structure, recommend integration steps, output a numbered change plan. Do NOT modify files. | No |
| `review` | Inspect an existing Splunk RUM integration for correctness, security, performance, and completeness. Output findings as a checklist. | No |
| `apply` | Execute the plan: add dependencies, write initialization code, configure modules. Follow the reference files exactly. | Yes |
| `verify` | Run build, check Logcat output, inspect agent status, confirm data reaches Splunk. | Read + shell |

Mode transitions:
- **New integration:** `plan` → user approves → `apply` → `verify`
- **Existing integration detected:** Phase 1 routes directly to `review` — run the Phase 4 checklist, report PASS/WARN/FAIL for each item, then ask the user what to change or add

---

## Instrumentation Depth

When planning or applying, scope work to one of three depth levels. Default to **baseline** for new integrations unless the user requests more.

| Depth | Includes |
|-------|---------|
| `baseline` | SDK dependency, `SplunkRum.install()`, realm/token via `BuildConfig`, manifest registration. Crash, ANR, startup, slow rendering, interactions, lifecycle, and network monitor are on by default. |
| `targeted` | Baseline + specific modules the user asks for: navigation tracking, OkHttp/HttpURLConnection instrumentation, custom events, mapping file upload. |
| `comprehensive` | Targeted + Session Replay setup, privacy/sensitivity configuration, `spanInterceptor` for PII filtering, WebView bridge, all applicable build-time plugins. |

---

## Hard Gates

These rules are non-negotiable regardless of mode or depth.

1. **Never commit secrets.** Realm, `rumAccessToken`, and `apiAccessToken` must come from `BuildConfig` fields injected via `gradle.properties`, environment variables, or the app's existing config pattern. If the app has no config pattern, create one using `gradle.properties` + `BuildConfig`.
2. **Never add dependencies the user did not approve.** The SDK artifact is `com.splunk:splunk-otel-android`. Build-time plugins (`com.splunk.rum-okhttp3-auto-plugin`, `com.splunk.rum-httpurlconnection-auto-plugin`, `com.splunk.rum-mapping-file-plugin`) must be listed in the plan and accepted before applying.
3. **Never overwrite existing observability setup without explicit confirmation.** If Sentry, Datadog, New Relic, Firebase Crashlytics, or a manual OpenTelemetry SDK is present, call out the conflict and wait for the user to decide.
4. **Never enable Session Replay without a privacy discussion.** Session Replay records visual content. Before enabling, confirm sensitivity configuration for PII-bearing views and appropriate `samplingRate`.
5. **Preserve the app's existing architecture.** Match build DSL (Kotlin vs Groovy), version catalog usage, dependency style, and source language. Do not introduce patterns the project does not already use.
6. **Never set `enableDebugLogging = true` in production paths.** Use `BuildConfig.DEBUG` to gate it.
7. **Never add `globalAttributes` containing PII** (user email, phone, device ID) unless the user explicitly requests it and understands the implications.

---

## Phase 1: Detect

Run these commands to understand the project before making any recommendations:

```bash
# Build system: Groovy vs Kotlin DSL
ls build.gradle build.gradle.kts settings.gradle settings.gradle.kts 2>/dev/null

# App module location
ls app/build.gradle app/build.gradle.kts 2>/dev/null

# Version catalog
ls gradle/libs.versions.toml 2>/dev/null

# Existing Splunk / OTel / competing SDKs
rg -i 'splunk|opentelemetry|com\.splunk|io\.opentelemetry' build.gradle* app/build.gradle* gradle/libs.versions.toml 2>/dev/null | head -15
rg -i 'sentry|datadog|newrelic|firebase.crashlytics|bugsnag|instabug' build.gradle* app/build.gradle* 2>/dev/null | head -10

# Android SDK versions
rg 'minSdk|targetSdk|compileSdk' app/build.gradle* 2>/dev/null | head -6

# Kotlin vs Java source
find app/src/main -name "*.kt" 2>/dev/null | head -3
find app/src/main -name "*.java" 2>/dev/null | head -3

# Compose detection
rg 'compose|androidx\.compose' app/build.gradle* 2>/dev/null | head -5

# OkHttp / Retrofit
rg -i 'okhttp|retrofit' app/build.gradle* 2>/dev/null | head -5

# Jetpack Navigation
rg 'androidx.navigation' app/build.gradle* 2>/dev/null | head -3

# Fragments
rg 'fragment' app/build.gradle* 2>/dev/null | head -3

# WebView usage
rg -r 'WebView' app/src/ 2>/dev/null | head -5

# Application class
find app/src/main -name "*.kt" -o -name "*.java" 2>/dev/null | xargs rg -l 'Application\(\)' 2>/dev/null | head -3

# Existing SplunkRum initialization
rg 'SplunkRum' app/src/ 2>/dev/null | head -5

# AndroidManifest
rg 'android:name' app/src/main/AndroidManifest.xml 2>/dev/null | head -5
```

**What to determine:**

| Question | Impact |
|----------|--------|
| `build.gradle.kts` present? | Use Kotlin DSL syntax in all examples |
| `gradle/libs.versions.toml` present? | Add Splunk to the version catalog |
| `minSdk < 24`? | SDK requires API 24+; API 21–23 experimental via `forceEnableOnLowerApi` |
| Compose detected? | Recommend `Modifier.splunkRum()` for session replay and compose navigation tracking |
| OkHttp/Retrofit present? | Recommend OkHttp auto-instrumentation plugin or manual wrapping |
| Jetpack Navigation? | Recommend `NavigationModuleConfiguration(isAutomatedTrackingEnabled = true)` + `registerNavController()` |
| Fragments? | Automated navigation tracking covers Fragment lifecycle |
| WebView present? | Recommend `webViewNativeBridge.integrateWithBrowserRum(webView)` |
| Application subclass exists? | That's where `SplunkRum.install()` goes |
| Already has `SplunkRum.install()`? | **Stop. Do not plan a new integration.** Switch to `review` mode and run the Phase 4 checklist. |
| Competing SDK present? | Call out conflicts; do not silently overwrite (Hard Gate #3) |

> **When `SplunkRum.install()` is already present:** Do not offer to "add" Splunk RUM. The integration exists. Do not list default-on modules as "missing" — modules like Crash, ANR, Startup, SlowRendering, Interactions, NetworkMonitor, Lifecycle, and ApplicationLifecycle activate automatically without explicit `ModuleConfiguration` objects. Only flag a module as missing if the user's app needs a non-default-on feature (Session Replay, build-time plugins, custom events) that is not configured.

### Phase 1 → Routing

After detection, follow exactly one path:

| Detection result | Next step |
|-----------------|-----------|
| **No existing `SplunkRum.install()`** | Continue to Phase 2 (Recommend) → Phase 3 (Guide) |
| **Existing `SplunkRum.install()` found** | **Skip Phases 2–3 entirely. Go directly to Phase 4 (Review Existing Integration).** Run every checklist item in Phase 4 against the code. Report passing and failing checks. Then ask the user what they want to change or add. |

---

## Phase 2: Recommend

Present a concrete recommendation based on detection. Lead with a proposal, not open questions.

**Core (default-on, always recommend):**
- **Crash Reporting** — uncaught exceptions with stack traces
- **ANR Detection** — application-not-responding events
- **App Startup** — cold/warm/hot start timing
- **Slow Rendering** — frozen/slow frame detection
- **Interactions** — tap, focus, keyboard, rage tap events
- **Network Monitor** — connectivity changes, carrier info on all spans
- **Lifecycle** — Activity/Fragment lifecycle events

**Recommended (configure per project):**
- **Screen/Navigation Tracking** — automatic or manual screen name tracking
- **Network Instrumentation** — OkHttp auto/manual, HttpURLConnection auto
- **Custom Events** — business events, workflow timing, error tracking

**Optional (opt-in):**
- **Session Replay** — visual recording of user sessions (requires explicit `start()`)
- **WebView Bridge** — link browser RUM to native Android session
- **Mapping File Upload** — ProGuard/R8 mapping upload for readable stack traces

**Recommendation logic:**

| Feature | Recommend when... |
|---------|-------------------|
| OkHttp auto plugin | App uses OkHttp and build-time bytecode weaving is acceptable |
| OkHttp manual | App uses OkHttp but plugin not acceptable; wrap clients manually |
| HttpURLConnection plugin | App uses `HttpURLConnection` (legacy networking) |
| Navigation auto tracking | App uses Fragments, Activities as screens, or Compose Navigation |
| Session Replay | User-facing production app; privacy review completed |
| WebView bridge | App embeds WebViews with Splunk Browser RUM |
| Mapping file plugin | Release builds with minification (ProGuard/R8) |

Propose: *"For your [Kotlin/Java] Android app (minSdk X), I recommend setting up the core SDK with [detected network stack] instrumentation and [auto/manual] navigation tracking. Want me to also configure Session Replay and mapping file upload?"*

---

## Phase 3: Guide

### Determine Setup Path

| Scenario | Path |
|----------|------|
| New integration, no existing Splunk setup | Full setup (dependency + init + manifest) |
| Existing `SplunkRum.install()` present | Jump to feature configuration |
| Version catalog in use | Add to `libs.versions.toml` first |

### Reference Files

Load the appropriate reference for each agreed feature. Each reference file includes its own "Load when" / "Do not load when" rules, source files to verify against, and step-by-step instructions.

| Feature | Reference | Load when... |
|---------|-----------|-------------|
| SDK Installation & Init | `skill-references/installation.md` | Always (baseline) |
| Screen/Navigation | `skill-references/screen-navigation.md` | App has multiple screens |
| Network Instrumentation | `skill-references/network-instrumentation.md` | App makes HTTP requests |
| Custom Events | `skill-references/custom-events.md` | Business event tracking needed |
| Session Replay | `skill-references/session-replay.md` | Visual session recording requested |
| Crash, ANR & Symbolication | `skill-references/crash-and-symbolication.md` | Tuning crash/ANR defaults or setting up mapping upload |
| Verification | `skill-references/verification-troubleshooting.md` | After any setup |

For each feature: load the reference, follow steps exactly, verify before moving on.

---

## Phase 4: Review Existing Integration

**When to use:** Phase 1 detected an existing `SplunkRum.install()` call. You were routed here by the Phase 1 → Routing table.

**What to do:** Read the app's initialization code and configuration. Run **every** checklist item below. For each item, report the result as one of:
- **PASS** — the check is satisfied (state the value you found)
- **WARN** — not a hard failure but worth calling out (explain why)
- **FAIL** — a problem that should be fixed (explain what's wrong and how to fix it)

After completing the checklist, present a summary table of all results, then ask the user what they'd like to change, fix, or add.

**Example output format:**
```
| Check                        | Result | Detail                                      |
|------------------------------|--------|---------------------------------------------|
| install() in onCreate()      | PASS   | Called in App.onCreate()                    |
| enableDebugLogging           | WARN   | Set to `true` — disable for production      |
| Token safety                 | PASS   | Uses BuildConfig from gradle.properties     |
| Session replay sampling      | WARN   | 50% may be high for production traffic      |
```

### Checklist

#### Initialization
- [ ] `SplunkRum.install()` called in `Application.onCreate()`
- [ ] Application class registered in `AndroidManifest.xml` via `android:name`
- [ ] `deploymentEnvironment` is not blank
- [ ] `enableDebugLogging` is `false` in production builds (WARN if `true` — acceptable for sample/debug apps)

#### Token/Secret Safety
- [ ] `rumAccessToken` not hardcoded in source — should use `BuildConfig` fields injected from `gradle.properties` or CI secrets
- [ ] `apiAccessToken` (mapping plugin) not committed — should use env var `SPLUNK_ACCESS_TOKEN` or `gradle.properties`
- [ ] No tokens in version control history

#### Duplicate/Conflicting Setup
- [ ] Only one `SplunkRum.install()` call (it returns existing instance if called twice, but flag as code smell)
- [ ] No competing observability SDKs (Sentry, Datadog, New Relic) without explicit awareness
- [ ] No manual OTel SDK setup conflicting with Splunk RUM's embedded OTel

#### Module Configuration
- [ ] Network instrumentation matches the app's HTTP stack (OkHttp auto vs manual vs HttpURLConnection)
- [ ] Navigation tracking enabled if app has multiple screens
- [ ] Session replay `start()` called after `install()` if replay is configured
- [ ] `SessionReplayModuleConfiguration.samplingRate` set to a reasonable value (not 1.0 in production)

#### Privacy
- [ ] Session replay sensitivity configured for PII-containing views (`EditText` is sensitive by default)
- [ ] Global attributes do not contain PII unless intended
- [ ] `spanInterceptor` used to filter sensitive span data if needed

#### Performance
- [ ] `SlowRenderingModuleConfiguration.interval` not set too aggressively (default `Duration.ofSeconds(1)` is fine; sub-100ms is risky)
- [ ] Session sampling rate appropriate for traffic volume
- [ ] `deferredUntilForeground` considered for apps with heavy background startup

---

## Configuration Quick Reference

### `AgentConfiguration`

| Property | Type | Default | Purpose |
|----------|------|---------|---------|
| `endpoint` | `EndpointConfiguration?` | `null` | Realm + token or custom URLs |
| `appName` | `String` | — | **Required.** Application name in Splunk |
| `deploymentEnvironment` | `String` | — | **Required.** e.g., `"prod"`, `"staging"` |
| `appVersion` | `String?` | `null` | App version string |
| `enableDebugLogging` | `Boolean` | `false` | Verbose SDK logs. **Never in production** |
| `globalAttributes` | `Attributes` | empty | Attributes appended to all spans |
| `spanInterceptor` | `(SpanData) -> SpanData?` | `null` | Filter/modify spans; return null to drop |
| `user` | `UserConfiguration` | `ANONYMOUS_TRACKING` | `NO_TRACKING` or `ANONYMOUS_TRACKING` |
| `session` | `SessionConfiguration` | 100% | `samplingRate` (0.0–1.0) |
| `deferredUntilForeground` | `Boolean` | `false` | Delay tracing until app foregrounds |
| `forceEnableOnLowerApi` | `Boolean` | `false` | Enable on API 21–23 (experimental) |

### Module Configurations

> **"Default On" means no configuration needed.** Modules marked "Yes" below activate automatically when the SDK installs — the app does **not** need to pass a `ModuleConfiguration` object for them. Only create a configuration object when you need to change a default (e.g., disable a module or tune an interval). Never flag a default-on module as "missing" just because the app's `install()` call omits its config class.

| Module | Config Class | Default On? |
|--------|-------------|-------------|
| Crash | `CrashModuleConfiguration(isEnabled)` | Yes |
| ANR | `AnrModuleConfiguration(isEnabled)` | Yes |
| App Startup | `StartupModuleConfiguration()` | Yes |
| Slow Rendering | `SlowRenderingModuleConfiguration(isEnabled, interval: Duration)` | Yes |
| Interactions | `InteractionsModuleConfiguration(isEnabled)` | Yes |
| Network Monitor | `NetworkMonitorModuleConfiguration(isEnabled)` | Yes |
| Lifecycle | `LifecycleModuleConfiguration(isEnabled, allowedEvents)` | Yes |
| App Lifecycle | `ApplicationLifecycleModuleConfiguration(isEnabled)` | Yes |
| Navigation | `NavigationModuleConfiguration(isEnabled, isAutomatedTrackingEnabled)` | On; auto off |
| OkHttp3 Auto | `OkHttp3AutoModuleConfiguration(isEnabled, headers...)` | Yes |
| OkHttp3 Manual | `OkHttp3ManualModuleConfiguration(headers...)` | Yes |
| HttpURLConnection | `HttpURLModuleConfiguration(isEnabled, headers...)` | Yes |
| Session Replay | `SessionReplayModuleConfiguration(isEnabled, samplingRate)` | No |

---

## Troubleshooting Quick Reference

| Issue | Solution |
|-------|----------|
| Events not appearing | Set `enableDebugLogging = true`; check Logcat for SDK errors; verify realm and token |
| `SplunkRum.install()` not called | Confirm `android:name=".YourApp"` in `AndroidManifest.xml` |
| Crash stack traces obfuscated | Set up mapping file plugin — see `skill-references/crash-and-symbolication.md` |
| OkHttp spans not appearing | Either apply `com.splunk.rum-okhttp3-auto-plugin` or manually wrap with `okHttpManualInstrumentation.buildOkHttpCallFactory(client)` |
| Session replay not recording | Ensure `SessionReplayModuleConfiguration(isEnabled = true)` AND call `sessionReplay.start()` after install |
| Navigation/screen names missing | Enable `NavigationModuleConfiguration(isAutomatedTrackingEnabled = true)` or call `navigation.track(screenName)` manually |
| Build failure: plugin not found | Add plugin to project-level `build.gradle.kts` with `apply false`; verify version matches SDK |
| `deploymentEnvironment` blank error | `deploymentEnvironment` must be a non-blank string |
| API 21–23 not working | Set `forceEnableOnLowerApi = true` (experimental) |
| Competing SDK conflict | Check for duplicate OTel SDK initialization; Splunk RUM embeds its own OTel instance |

For detailed troubleshooting: load `skill-references/verification-troubleshooting.md`
