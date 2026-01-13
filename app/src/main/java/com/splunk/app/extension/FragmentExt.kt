/*
 * Copyright 2026 Splunk Inc.
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

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.splunk.app.R
import com.splunk.app.util.ApiVariant

fun Fragment.showDoneToast(
    @StringRes labelRes: Int,
    apiVariant: ApiVariant = ApiVariant.LATEST,
    duration: Int = Toast.LENGTH_SHORT
) {
    showDoneToast(getString(labelRes), apiVariant, duration)
}

fun Fragment.showDoneToast(
    label: String,
    apiVariant: ApiVariant = ApiVariant.LATEST,
    duration: Int = Toast.LENGTH_SHORT
) {
    val message = when (apiVariant) {
        ApiVariant.LATEST -> getString(R.string.toast_action_done, label)
        ApiVariant.LEGACY -> getString(R.string.toast_legacy_action_done, label)
    }

    showToast(message, duration)
}

fun Fragment.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    runOnUiThread {
        Toast.makeText(context ?: return@runOnUiThread, message, duration).show()
    }
}

fun Fragment.showToast(@StringRes messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
    runOnUiThread {
        Toast.makeText(context ?: return@runOnUiThread, messageResId, duration).show()
    }
}