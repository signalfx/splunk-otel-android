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
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.smartlook.app.R

class ColoredLinearLayout(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ColoredLinearLayout)
            val count = a.getInt(R.styleable.ColoredLinearLayout_colored_itemsCount, 0)
            val itemWidth = a.getDimension(R.styleable.ColoredLinearLayout_colored_itemWidth, 0f)
            val itemHeight = a.getDimension(R.styleable.ColoredLinearLayout_colored_itemHeight, 0f)
            a.recycle()

            createItems(count, itemWidth.toInt(), itemHeight.toInt())
        }
    }

    private fun createItems(count: Int, width: Int, height: Int) {
        for (i in 0 until count) {
            val view = View(context)
            view.setBackgroundColor(COLORS[i % COLORS.size])
            addView(view, LayoutParams(width, height))
        }
    }

    companion object {
        private val COLORS = intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt())
    }
}
