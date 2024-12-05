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

package com.smartlook.app.ui.interaction

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import com.smartlook.sdk.common.utils.dpToPx
import java.util.UUID

class FocusActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = FrameLayout(this)
        val field = EditText(this)

        val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, dpToPx(50f))
        layoutParams.topMargin = -dpToPx(200f) + (Math.random() * dpToPx(400f)).toInt()
        layoutParams.gravity = Gravity.CENTER

        layout.addView(field, layoutParams)

        setContentView(layout)

        field.setText(UUID.randomUUID().toString())
        layout.setOnClickListener(onClickListener)
    }

    private val onClickListener = View.OnClickListener {
        startActivity(Intent(this, FocusActivity::class.java))
    }
}
