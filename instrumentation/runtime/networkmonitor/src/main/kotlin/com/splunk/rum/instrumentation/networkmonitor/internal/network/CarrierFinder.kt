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

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.splunk.rum.instrumentation.networkmonitor.internal.model.Carrier

internal class CarrierFinder(private val context: Context, private val telephonyManager: TelephonyManager?) {
    fun get(): Carrier? {
        val manager = telephonyManager
        if (manager == null) {
            Log.w(
                TAG,
                "Cannot determine carrier details: telephony service unavailable."
            )
            return null
        }
        if (!hasTelephonyFeature(context)) {
            Log.w(
                TAG,
                "Cannot determine carrier details: telephony feature missing."
            )
            return null
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (hasPhoneStatePermission(context)) {
                    getCarrierPostApi28(manager)
                } else {
                    Log.w(
                        TAG,
                        "Missing read phone state permission, using legacy carrier methods."
                    )
                    getCarrierPreApi28(manager)
                }
            } else {
                getCarrierPreApi28(manager)
            }
        } catch (exception: SecurityException) {
            Log.w(TAG, "SecurityException when accessing carrier info.", exception)
            null
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Failed to access carrier info.", exception)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getCarrierPostApi28(manager: TelephonyManager): Carrier {
        val carrierName = manager.simCarrierIdName?.takeIf { it.isNotEmpty() }?.toString()
        val (mcc, mnc, iso) = getMccMncIso(manager)
        return Carrier(
            id = manager.simCarrierId,
            name = carrierName,
            mobileCountryCode = mcc,
            mobileNetworkCode = mnc,
            isoCountryCode = iso
        )
    }

    private fun getCarrierPreApi28(manager: TelephonyManager): Carrier {
        val carrierName = manager.simOperatorName?.takeIf { it.isNotEmpty() }
            ?: manager.networkOperatorName?.takeIf { it.isNotEmpty() }
        val (mcc, mnc, iso) = getMccMncIso(manager)
        return Carrier(
            name = carrierName,
            mobileCountryCode = mcc,
            mobileNetworkCode = mnc,
            isoCountryCode = iso
        )
    }

    private fun getMccMncIso(manager: TelephonyManager): Triple<String?, String?, String?> {
        val simOperator = manager.simOperator
        val mcc = simOperator?.takeIf { it.length >= 5 }?.take(3)
        val mnc = simOperator?.takeIf { it.length >= 5 }?.substring(3)
        val iso = manager.simCountryIso?.takeIf { it.isNotEmpty() }
        return Triple(mcc, mnc, iso)
    }

    private companion object {
        private const val TAG = "CarrierFinder"
    }
}
