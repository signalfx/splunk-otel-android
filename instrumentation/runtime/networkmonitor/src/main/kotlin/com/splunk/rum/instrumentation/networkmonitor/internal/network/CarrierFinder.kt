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
import androidx.annotation.RequiresApi
import com.splunk.rum.common.logger.Logger
import com.splunk.rum.instrumentation.networkmonitor.internal.model.Carrier

internal class CarrierFinder(private val context: Context, private val telephonyManager: TelephonyManager?) {
    fun get(): Carrier? {
        val manager = telephonyManager
        if (manager == null) {
            Logger.w(
                TAG,
                "Cannot determine carrier details: telephony service unavailable."
            )
            return null
        }
        return try {
            if (!hasTelephonySubscriptionFeature(context)) {
                Logger.w(
                    TAG,
                    "Cannot determine carrier details: telephony subscription feature missing."
                )
                null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                getCarrierPostApi28(manager)
            } else {
                getCarrierPreApi28(manager)
            }
        } catch (exception: SecurityException) {
            Logger.w(TAG, "SecurityException when accessing carrier info.", exception)
            null
        } catch (exception: RuntimeException) {
            Logger.w(TAG, "Failed to access carrier info.", exception)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getCarrierPostApi28(manager: TelephonyManager): Carrier? {
        val carrierName = try {
            manager.simCarrierIdName?.takeIf { it.isNotEmpty() }?.toString()
        } catch (exception: SecurityException) {
            Logger.w(TAG, "SecurityException when accessing modern carrier name; trying legacy lookup.", exception)
            getLegacyCarrierName(manager)
        } catch (exception: RuntimeException) {
            Logger.w(TAG, "Failed to access modern carrier name; trying legacy lookup.", exception)
            getLegacyCarrierName(manager)
        }
        val (mcc, mnc, iso) = getMccMncIso(manager)
        return Carrier(
            name = carrierName,
            mobileCountryCode = mcc,
            mobileNetworkCode = mnc,
            isoCountryCode = iso
        )
    }

    private fun getCarrierPreApi28(manager: TelephonyManager): Carrier {
        val carrierName = getLegacyCarrierName(manager)
        val (mcc, mnc, iso) = getMccMncIso(manager)
        return Carrier(
            name = carrierName,
            mobileCountryCode = mcc,
            mobileNetworkCode = mnc,
            isoCountryCode = iso
        )
    }

    private fun getLegacyCarrierName(manager: TelephonyManager): String? =
        manager.simOperatorName?.takeIf { it.isNotEmpty() }
            ?: manager.networkOperatorName?.takeIf { it.isNotEmpty() }

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
