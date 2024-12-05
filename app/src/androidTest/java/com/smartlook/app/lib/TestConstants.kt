/*
 * Copyright 2024 Splunk Inc.
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

package com.smartlook.app.lib

import com.smartlook.app.BuildConfig

object TestConstants {
    const val DEFAULT_NETWORK_TIMEOUT = 120_000L
    const val DEFAULT_MELT_TIMEOUT = 20_000L
    const val ZIPKIN_URL = BuildConfig.IP_ADDRESS
}
