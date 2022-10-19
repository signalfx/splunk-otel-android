/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal.instrumentation;

import io.opentelemetry.rum.internal.OpenTelemetryRum;

/**
 * Implementations of this interface may install instrumentations using the passed {@link
 * InstrumentedApplication} instance as part of the {@link OpenTelemetryRum} construction process.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@FunctionalInterface
public interface InstrumentationInstaller {

    /** Installs an instrumentation on the given {@code instrumentedApplication}. */
    void install(InstrumentedApplication instrumentedApplication);
}
