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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import com.smartlook.sdk.common.utils.dpToPxF

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class RuntimeShaderView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint()

    init {
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = dpToPxF(15f)
        paint.color = Color.RED
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (paint.shader == null) {
                val shader = RuntimeShader(SHADER_PROGRAM)

                shader.setColorUniform("startColor", Color.BLUE)
                shader.setColorUniform("endColor", Color.RED)
                shader.setFloatUniform("size", width.toFloat(), height.toFloat())

                paint.shader = shader
            }

            canvas.drawPaint(paint)
        } else
            canvas.drawText("Android 13+", width / 2f, (height + paint.descent() - paint.ascent()) / 2f, paint)
    }

    private companion object {
        const val SHADER_PROGRAM = """
layout(color) uniform vec4 startColor;
layout(color) uniform vec4 endColor;

uniform vec2 size;
            
vec4 main(vec2 canvas_coordinates) {
    vec2 coors = canvas_coordinates / size;
    return mix(startColor, endColor, coors.x);
}
        """
    }
}
