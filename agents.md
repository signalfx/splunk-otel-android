# AI Agent Guidelines for Splunk Android RUM SDK

This guide provides essential information for AI agents working within the `splunk-otel-android` repository.

---

## ⚠️ Critical Rules

### 1. NO New External Dependencies

**Under NO circumstances should you add any new dependency to the SDK.**

- The Splunk RUM SDK is designed to be **lightweight** and must minimize its footprint in customer applications
- Adding dependencies increases artifact size and can introduce version conflicts for SDK consumers
- All allowed dependencies are defined in `buildSrc/src/main/kotlin/Dependencies.kt`
- Even dependencies that exist in `Dependencies.kt` should only be used if **explicitly requested** by the user
- If a task seems to require a new dependency, **stop and ask the user** for an alternative approach

### 2. Public API Immutability

**The public API is stable and must NOT be changed without explicit user confirmation.**

#### What is Public API?
- **`:agent` module** (`agent/`) — The main entry point for SDK consumers
- **`:integration` modules** (`integration/`) — Any module under the integration folder that `:agent` imports as an `api` dependency

#### What constitutes a Public API change?
- Method/function signatures (name, parameters, defaults, return types)
- Behavior, semantics, and side effects
- Exceptions/errors that can be raised
- Public constants and exported symbols
- Request/response schemas and data classes
- Visibility changes (public → private/internal)
- Deprecations or removals

#### The Rule
> **Default assumption: Public API is immutable unless the user explicitly asks for the change and confirms it.**

If a task appears to require changing any public method in `agent/` or `integration/`:
1. **STOP** and inform the user
2. **Document** what would need to change and why
3. **Wait** for explicit user confirmation before proceeding
4. Explore alternative approaches that don't modify public API

### 3. Backwards Compatibility

**All changes must maintain backwards compatibility.**

- SDK consumers should be able to update to new versions without code changes
- Deprecated APIs must continue to function (mark with `@Deprecated` annotation with migration guidance)
- Default behaviors must not change in ways that break existing integrations
- New features should be opt-in, not opt-out

---

## Project Overview

This repository contains the source code for the **Splunk Android RUM (Real User Monitoring) SDK**. It is an open-source, multi-module Gradle project written in Kotlin, built on OpenTelemetry.

The SDK provides instrumentation for:
- ANR Detection
- App Startup Tracking
- Crash Reporting
- Custom Event Tracking
- Navigation Tracking
- Network Monitoring
- HTTP Request Instrumentation (OkHttp3, HttpURLConnection)
- Session Replay
- Slow/Frozen Rendering Detection
- User Interaction Tracking
- WebView Integration

---

## Project Structure

```
splunk-otel-android/
├── agent/                      # Main SDK entry point (PUBLIC API)
├── integration/                # Feature modules (PUBLIC API where exposed via agent)
│   ├── agent/
│   │   ├── api/               # Public API interfaces
│   │   ├── common/            # Shared agent code
│   │   └── internal/          # Internal implementation
│   ├── anr/                   # ANR detection
│   ├── applicationlifecycle/  # App lifecycle tracking
│   ├── crash/                 # Crash reporting
│   ├── customtracking/        # Custom events API
│   ├── httpurlconnection-auto/# Auto HttpURLConnection instrumentation
│   ├── interactions/          # User interaction tracking
│   ├── navigation/            # Screen navigation tracking
│   ├── networkmonitor/        # Network state monitoring
│   ├── okhttp3-auto/          # Auto OkHttp3 instrumentation
│   ├── okhttp3-common/        # Shared OkHttp3 code
│   ├── okhttp3-manual/        # Manual OkHttp3 instrumentation
│   ├── sessionreplay/         # Session replay
│   ├── slowrendering/         # Slow rendering detection
│   ├── startup/               # App startup tracking
│   └── webview/               # WebView integration
├── common/                     # Shared utilities (internal)
│   ├── otel/                  # OpenTelemetry helpers
│   ├── storage/               # Storage utilities
│   └── utils/                 # General utilities
├── instrumentation/            # Build-time instrumentation
│   ├── buildtime/             # Gradle plugins for auto-instrumentation
│   └── runtime/               # Runtime instrumentation hooks
├── buildSrc/                   # Build configuration and dependencies
└── app/                        # Sample application
```

---

## Building and Running

The project uses Gradle with the wrapper script `./gradlew`.

### Building

```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :<module>:build

# Examples:
./gradlew :agent:build
./gradlew :integration:crash:build
```

### Running Tests

#### Unit Tests (JVM)
```bash
./gradlew :<module>:check

# Examples:
./gradlew :agent:check
./gradlew :integration:navigation:check
```

#### Integration Tests (Device/Emulator)
```bash
./gradlew :<module>:connectedCheck
```

---

## Development Conventions

### Code Formatting

The project uses **ktlint** for code formatting. Always format before committing:

```bash
# Format entire project
./gradlew ktlintFormat

# Format specific module
./gradlew :<module>:ktlintFormat
```

### Kotlin Style
- Follow standard Kotlin coding conventions
- Use meaningful names for classes, functions, and variables
- Document public APIs with KDoc comments
- Prefer immutability (`val` over `var`, immutable collections)
- Use `internal` visibility for SDK-internal code that shouldn't be exposed

### Testing
- Write unit tests for all new functionality
- Tests should be focused and test one behavior each
- Use descriptive test names that explain the scenario
- Mock external dependencies appropriately

---

## Iteration Loop

After making any change, follow this workflow:

### 1. Format Code
```bash
./gradlew :<module>:ktlintFormat
```

### 2. Run Unit Tests
```bash
./gradlew :<module>:check
```

### 3. Run Integration Tests (if applicable)
```bash
./gradlew :<module>:connectedCheck
```

### 4. Verify Build
```bash
./gradlew :<module>:build
```

---

## Allowed Changes (Without User Approval)

You MAY proceed without explicit approval for:
- Internal refactors that do **not** change public behavior
- Bug fixes that preserve existing API contracts
- Performance improvements with identical external behavior
- Adding/updating tests
- Documentation improvements
- Comments and code clarity improvements
- Private/internal implementation details

---

## Changes Requiring User Confirmation

**ALWAYS stop and ask** before:
- Adding ANY new dependency (even if it exists in Dependencies.kt)
- Modifying any public API in `agent/` or `integration/`
- Changing default behaviors
- Deprecating existing functionality
- Removing any code that might be used by SDK consumers
- Making changes that could break backwards compatibility

---

## Module Guidelines

### Working with `agent/` Module
- This is the main entry point consumers use
- Every public class/method is part of the stable API
- Changes here have the highest impact on consumers

### Working with `integration/` Modules
- Feature-specific instrumentation modules
- Public APIs exposed through `:agent` must be stable
- Internal implementation can be modified freely

### Working with `common/` Modules
- Shared utilities for internal SDK use
- Not exposed to consumers
- Can be modified more freely, but consider impact on dependent modules

### Working with `instrumentation/` Modules
- Build-time and runtime instrumentation
- Gradle plugins for automatic instrumentation
- Changes affect how the SDK integrates with customer build processes

---

## Quick Reference

| Action | Allowed? | Notes |
|--------|----------|-------|
| Add new dependency | ❌ NO | Never without explicit user request |
| Modify public API | ❌ ASK | Requires user confirmation |
| Change default behavior | ❌ ASK | Could break existing integrations |
| Internal refactoring | ✅ YES | If it doesn't affect public behavior |
| Add tests | ✅ YES | Always encouraged |
| Fix bugs (API-preserving) | ✅ YES | Maintain backwards compatibility |
| Performance optimization | ✅ YES | If behavior is identical |
| Update documentation | ✅ YES | Always encouraged |

---

## Updating This Guide

If you discover new patterns, conventions, or important information that would help future AI agents work with this repository, update this guide accordingly.
