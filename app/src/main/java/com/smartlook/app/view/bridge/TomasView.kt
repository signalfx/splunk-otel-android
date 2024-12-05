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

package com.smartlook.app.view.bridge

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class TomasView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private val textureView = TomasTextureView(context)

    val elements: List<TomasElement>
        get() = textureView.elements

    var listener: Listener? = null

    init {
        textureView.listener = TomasTextureViewListener()
        addView(textureView)
    }

    private inner class TomasTextureViewListener : TomasTextureView.Listener {
        override fun onTransitionChanged(isRunning: Boolean) {
            listener?.onTransitionChanged(isRunning)
        }
    }

    interface Listener {
        fun onTransitionChanged(isRunning: Boolean)
    }
}
