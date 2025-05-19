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

package com.splunk.app.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.cisco.android.common.utils.runOnUiThread
import com.splunk.app.R

object CommonUtils {
    fun showDoneToast(context: Context?, message: String) {
        if (context == null) {
            Log.e("CommonUtils", "Context is null while attempting to show a toast.")
            return
        }
        runOnUiThread {
            Toast.makeText(context, context.getString(R.string.http_toast, message), Toast.LENGTH_SHORT).show()
        }
    }
}
