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
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.smartlook.sdk.common.utils.dpToPxF
import kotlin.math.max

class BarChartView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint()

    init {
        paint.textSize = LABEL_SIZE
    }

    override fun onDraw(canvas: Canvas) {
        val legendLeft = drawLegend(canvas)
        val axisRight = drawAxis(canvas, legendLeft)
        drawChart(canvas, axisRight, legendLeft)
    }

    @Suppress("UnnecessaryVariable")
    private fun drawAxis(canvas: Canvas, legendLeft: Float): Float {
        val textHeight = paint.descent() - paint.ascent()

        val min = 0f
        val minLabel = min.toInt().toString()
        val minWidth = paint.measureText(minLabel)

        val max = ENTRIES.maxOf { it.values.maxOf { it.y } }
        val maxLabel = max.toInt().toString()
        val maxWidth = paint.measureText(maxLabel)

        val labelWidth = max(minWidth, maxWidth)

        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(minLabel, labelWidth, height - paddingBottom - CHART_VERTICAL_MARGIN - textHeight, paint)
        canvas.drawText(maxLabel, labelWidth, -paint.ascent(), paint)

        val verticalAxisLeft = labelWidth + LABEL_MARGIN
        val verticalAxisTop = paddingTop.toFloat()
        val verticalAxisRight = verticalAxisLeft + AXIS_LINE_WIDTH
        val verticalAxisBottom = height - paddingBottom - CHART_VERTICAL_MARGIN - textHeight

        paint.color = Color.LTGRAY
        canvas.drawRect(verticalAxisLeft, verticalAxisTop, verticalAxisRight, verticalAxisBottom, paint)

        val horizontalAxisLeft = verticalAxisRight
        val horizontalAxisTop = verticalAxisBottom - AXIS_LINE_WIDTH
        val horizontalAxisRight = legendLeft - CHART_HORIZONTAL_MARGIN
        val horizontalAxisBottom = verticalAxisBottom

        canvas.drawRect(horizontalAxisLeft, horizontalAxisTop, horizontalAxisRight, horizontalAxisBottom, paint)

        return verticalAxisRight
    }

    private fun drawChart(canvas: Canvas, axisRight: Float, legendLeft: Float) {
        val sections = ENTRIES.toSections()
        val bars = sections.toBars()

        val textHeight = paint.descent() - paint.ascent()

        val contentBottom = height - paddingBottom - CHART_VERTICAL_MARGIN - textHeight
        val contentWidth = legendLeft - axisRight - CHART_HORIZONTAL_MARGIN
        val contentHeight = height - paddingTop - paddingBottom

        val max = bars.maxOf { (it as? Bar.Colored)?.value ?: 0f }
        val barWidth = contentWidth / bars.size

        var sectionLeft: Float
        var sectionRight = axisRight

        val labelY = height - paddingBottom - paint.descent()

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.BLACK

        for (i in sections.indices) {
            val section = sections[i]

            sectionLeft = sectionRight
            sectionRight = sectionLeft + section.values.size * barWidth

            val labelX = sectionLeft + (sectionRight - sectionLeft) / 2f + i * barWidth

            canvas.drawText(section.x.toInt().toString(), labelX, labelY, paint)
        }

        for (i in bars.indices) {
            val bar = bars[i]

            if (bar is Bar.Colored) {
                val left = axisRight + i * barWidth
                val right = left + barWidth
                val top = paddingTop + contentHeight - (contentHeight * bar.value / max)

                paint.color = bar.color

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    canvas.drawRoundRect(left, top, right, contentBottom, CHART_CORNER_RADIUS, CHART_CORNER_RADIUS, paint)
                else
                    canvas.drawRect(left, top, right, contentBottom, paint)
            }
        }
    }

    private fun drawLegend(canvas: Canvas): Float {
        paint.textAlign = Paint.Align.LEFT

        val maxWidth = ENTRIES.maxOf { paint.measureText(it.label) }
        val textHeight = paint.descent() - paint.ascent()

        val legendHeight = ENTRIES.size * textHeight + (ENTRIES.size - 1) * LABEL_MARGIN
        val contentHeight = height - paddingTop - paddingBottom
        val legendTop = paddingTop + (contentHeight - legendHeight) / 2
        val legendEnd = paddingStart + width - paddingStart - paddingEnd

        for (i in ENTRIES.indices) {
            val entry = ENTRIES[i]
            val top = legendTop + i * (textHeight + LABEL_MARGIN)

            val labelX = legendEnd - maxWidth
            val labelY = top - paint.ascent()

            paint.color = Color.BLACK
            canvas.drawText(entry.label, labelX, labelY, paint)

            val rectRight = labelX - LABEL_MARGIN
            val rectLeft = rectRight - textHeight * LABEL_POINT_SIZE_RATIO
            val rectTop = top + textHeight * (1f - LABEL_POINT_SIZE_RATIO) / 2f
            val rectBottom = rectTop + textHeight * LABEL_POINT_SIZE_RATIO

            paint.color = entry.color
            canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, paint)
        }

        return legendEnd - (maxWidth + LABEL_MARGIN + textHeight * LABEL_POINT_SIZE_RATIO)
    }

    private fun List<Entry>.toSections(): List<Section> {
        val sections = HashMap<Float, ArrayList<Section.Value>>()

        for (entry in this)
            for (value in entry.values) {
                val values = sections[value.x] ?: ArrayList()
                values += Section.Value(value.y, entry.color)
                sections[value.x] = values
            }

        return sections.map { Section(it.key, it.value) }
    }

    private fun List<Section>.toBars(): List<Bar> {
        val bars = ArrayList<Bar>()

        for (section in this) {
            for (value in section.values)
                bars += Bar.Colored(value.value, value.color)

            bars += Bar.Empty()
        }

        bars.removeLast()
        return bars
    }

    private data class Entry(
        val label: String,
        val color: Int,
        val values: List<Value>
    ) {

        data class Value(
            val x: Float,
            val y: Float
        )
    }

    private data class Section(
        val x: Float,
        val values: List<Value>
    ) {

        data class Value(
            val value: Float,
            val color: Int
        )
    }

    @Suppress("CanSealedSubClassBeObject")
    private sealed class Bar {

        class Empty : Bar()

        data class Colored(
            val value: Float,
            val color: Int
        ) : Bar()
    }

    companion object {

        private val LABEL_SIZE = dpToPxF(10f)
        private val LABEL_MARGIN = dpToPxF(5f)

        private val CHART_HORIZONTAL_MARGIN = dpToPxF(15f)
        private val CHART_VERTICAL_MARGIN = dpToPxF(5f)

        private val CHART_CORNER_RADIUS = dpToPxF(5f)

        private val AXIS_LINE_WIDTH = dpToPxF(1f)

        private const val LABEL_POINT_SIZE_RATIO = 0.5f

        private val ENTRIES = listOf(
            Entry(
                "Apple", 0xff8fb9aa.toInt(),
                listOf(
                    Entry.Value(2012f, 25f),
                    Entry.Value(2013f, 23f),
                    Entry.Value(2014f, 27f),
                )
            ),
            Entry(
                "Banana", 0xfff2d096.toInt(),
                listOf(
                    Entry.Value(2012f, 30f),
                    Entry.Value(2013f, 32f),
                    Entry.Value(2014f, 35f),
                )
            ),
            Entry(
                "Orange", 0xffb2e7e8.toInt(),
                listOf(
                    Entry.Value(2012f, 35f),
                    Entry.Value(2013f, 32f),
                    Entry.Value(2014f, 33f),
                )
            ),
            Entry(
                "Blueberry", 0xff304d63.toInt(),
                listOf(
                    Entry.Value(2012f, 50f),
                    Entry.Value(2013f, 45f),
                    Entry.Value(2014f, 55f),
                )
            ),
            Entry(
                "Lemon", 0xffed8975.toInt(),
                listOf(
                    Entry.Value(2012f, 42f),
                    Entry.Value(2013f, 43f),
                    Entry.Value(2014f, 45f),
                )
            )
        )
    }
}
