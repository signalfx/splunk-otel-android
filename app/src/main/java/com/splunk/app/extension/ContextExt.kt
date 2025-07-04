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

package com.splunk.app.extension

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.cisco.android.common.utils.runOnUiThread

fun Context.showToast(@StringRes resource: Int, duration: Int = Toast.LENGTH_SHORT) =
    showToast(getString(resource))

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) =
    runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }