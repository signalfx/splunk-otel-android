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

package com.smartlook.app.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import com.smartlook.app.databinding.FragmentDialogBinding

object AlertDialog {

    fun show(context: Context, isDimEnabled: Boolean = true) {
        val inflater = LayoutInflater.from(context)
        val viewBinding = FragmentDialogBinding.inflate(inflater, null, false)

        val dialog = AlertDialog.Builder(context)
            .setView(viewBinding.root)
            .show()

        viewBinding.button.setOnClickListener { dialog.dismiss() }

        if (!isDimEnabled)
            dialog.window?.setDimAmount(0f)
    }
}
