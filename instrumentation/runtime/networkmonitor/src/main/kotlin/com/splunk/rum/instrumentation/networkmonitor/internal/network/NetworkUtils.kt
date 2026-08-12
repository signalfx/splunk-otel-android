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

package com.splunk.rum.instrumentation.networkmonitor.internal.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager

internal fun hasPhoneStatePermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return true
    }
    val hasReadPhoneState =
        context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val hasBasicPhoneState =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.READ_BASIC_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    return hasReadPhoneState || hasBasicPhoneState
}

internal fun hasTelephonyFeature(context: Context): Boolean =
    context.packageManager?.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) == true

@Suppress("DEPRECATION")
internal fun getNetworkTypeName(networkType: Int): String = when (networkType) {
    // GSM and UMTS family.
    TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
    TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
    TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
    TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
    TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
    TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
    TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
    TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
    TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"

    // Deprecated CDMA family; retained for devices that still report these network types.
    TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
    TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
    TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
    TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
    TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
    TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
    TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"

    // LTE and newer radio technologies.
    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
    TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
    TelephonyManager.NETWORK_TYPE_NR -> "NR"

    else -> "UNKNOWN"
}
