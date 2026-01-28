# Agents

This guide provides essential information for working within the `splunk-otel-android` repository.

## Project Overview

This repository contains the source code for the Android Splunk RUM SDK. It is a large, multi-module
Gradle project. The project is written Kotlin.

The project is structured as a collection of libraries, each representing a product or a
shared component. These libraries are published as Maven artifacts to Maven Repository.

## Building and Running

The project is built using Gradle. The `gradlew` script is provided in the root directory.

### Building

To build the entire project, you can run the following command:

```bash
./gradlew build
```

To build a specific module, you can run:

```bash
./gradlew :<module>:build
```

### Running Tests

The project has two types of tests: unit tests and integration tests.

#### Unit Tests

Unit tests run on the local JVM. They can be executed with the following command:

```bash
./gradlew :<module>:check
```

#### Integration Tests

Integration tests run on a hardware device or emulator.

To run integration tests on a local emulator, use the following command:

```bash
./gradlew :<module>:connectedCheck
```

## Development Conventions

### Code Formatting

The project uses ktlint for code formatting. To format the code, run the following command:

```bash
./gradlew ktlintFormat
```

To format a specific project, run:

```bash
./gradlew :<module>:ktlintFormat
```

## Public API stability
The following areas are considered **Public API** and are **stable by default**:

* **Module:** `agent/` (the `:agent` module)
* **Dependencies used by `:agent`:** `integration/` (anything under the `:integration` folder that `:agent` imports/uses as `api` dependency)

### Rules
* **All public methods in `agent/` and `integration/` MUST NOT be modified.**
* This includes (but is not limited to):
    * Method/function signatures (name, parameters, defaults, return types)
    * Behavior/semantics and side effects
    * Exceptions/errors raised
    * Public constants and exported symbols
    * Request/response schemas


### Allowed changes (without explicit approval)
* Internal refactors that do **not** change public behavior
* Tests, docs, comments
* Performance improvements that preserve identical external behavior

#### Any change requires explicit user request + confirmation
If a task appears to require changing any public method in `agent/` or `integration/`, **stop and ask the user** to explicitly confirm they want the public API modified, and document what will change.

> Default assumption: **Public API is immutable unless the user explicitly asks for the change and confirms it.**

## External Dependencies

Do not add, under any circumstance, any new dependency to a SDK that does not already exists in the
`buildSrc/src/main/kotlin/Dependencies.kt`, and even then, only do it if explicitly asked to do so. The Splunk RUM
SDK is designed to be lightweight, and adding new dependencies can increase the size of the final
artifacts.

## Iteration Loop

After you make a change, here's the flow you should follow:

- Format the code using `ktlint`. It can be run with:

  ```bash
  ./gradlew :<module>:ktlintFormat
  ```
- Run unit tests:

  ```bash
  ./gradlew :<module>:check
  ```
- If necessary, run integration tests based on the instructions above.

## Updating this Guide

If new patterns or conventions are discovered, update this guide to ensure it remains a useful
resource.