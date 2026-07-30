/*
 * Copyright 2026 Splunk Inc.
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.networkmonitor.internal.model

internal data class CurrentNetwork(
    val state: NetworkState,
    private val carrier: Carrier? = null,
    val subType: String? = null
) {
    val carrierCountryCode: String?
        get() = carrier?.mobileCountryCode

    val carrierIsoCountryCode: String?
        get() = carrier?.isoCountryCode

    val carrierNetworkCode: String?
        get() = carrier?.mobileNetworkCode

    val carrierName: String?
        get() = carrier?.name
}
