/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.app.extension

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.cisco.android.common.utils.runOnUiThread
import com.splunk.app.R
import com.splunk.app.util.ApiVariant

fun Context.showDoneToast(
    @StringRes labelRes: Int,
    apiVariant: ApiVariant = ApiVariant.LATEST,
    duration: Int = Toast.LENGTH_SHORT
) {
    showDoneToast(getString(labelRes), apiVariant, duration)
}

fun Context.showDoneToast(
    label: String,
    apiVariant: ApiVariant = ApiVariant.LATEST,
    duration: Int = Toast.LENGTH_SHORT
) {
    val message = when (apiVariant) {
        ApiVariant.LATEST -> getString(R.string.toast_action_done, label)
        ApiVariant.LEGACY -> getString(R.string.toast_legacy_action_done, label)
    }

    runOnUiThread {
        Toast.makeText(this, message, duration).show()
    }
}