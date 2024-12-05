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

package com.smartlook.app.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SimpleCanvasView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint()

    override fun onDraw(canvas: Canvas) {
        paint.color = Color.RED
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.3f, paint)

        paint.color = Color.BLUE
        canvas.drawRect(0f, height * 0.3f, width.toFloat(), height.toFloat(), paint)

        paint.color = Color.GREEN
        canvas.drawRect(0.3f * width, 0f, 0.5f * width, height.toFloat(), paint)
    }
}
