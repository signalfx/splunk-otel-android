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
import com.smartlook.app.R
import com.smartlook.sdk.common.utils.dpToPxF

class ShadowView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var shadowColor = Color.GRAY
    private var fillColor = Color.WHITE
    private var textColor = Color.BLACK

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ShadowView)
            fillColor = a.getColor(R.styleable.ShadowView_shadow_fillColor, fillColor)
            shadowColor = a.getColor(R.styleable.ShadowView_shadow_shadowColor, shadowColor)
            a.recycle()
        }

        paint.setShadowLayer(dpToPxF(10f), dpToPxF(1f), dpToPxF(5f), shadowColor)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = dpToPxF(15f)

        textColor = if ((Color.red(fillColor) * 299 + Color.green(fillColor) * 587 + Color.blue(fillColor) * 114) / 1000 >= 128) Color.BLACK else Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = fillColor
        canvas.drawRoundRect(paddingLeft.toFloat(), paddingTop.toFloat(), (width - paddingRight).toFloat(), (height - paddingBottom).toFloat(), CORNER_RADIUS, CORNER_RADIUS, paint)

        paint.color = textColor
        canvas.drawText("Shadow", paddingLeft + (width - paddingLeft - paddingRight) / 2f, paddingTop + (height - paddingTop - paddingBottom) / 2f + paint.descent(), paint)
    }

    companion object {
        private val CORNER_RADIUS = dpToPxF(10f)
    }
}
