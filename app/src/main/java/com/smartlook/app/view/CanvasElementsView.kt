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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import com.smartlook.app.R
import com.smartlook.app.extension.add
import com.smartlook.app.extension.rotate
import com.smartlook.sdk.common.utils.dpToPxF
import kotlin.math.ceil
import kotlin.math.min

class CanvasElementsView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val choreographer = Choreographer.getInstance()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()

    private val elements = ArrayList<Element>()

    init {
        elements += FixedArcSweepElement(false)
        elements += FixedArcSweepElement(true)
        elements += VariableArcSweepAndAngleElement(false)
        elements += VariableArcSweepAndAngleElement(true)
        elements += DrawPathElement()
        elements += ColorElement()
        elements += TextElement()
        elements += RgbElement()
        elements += DrawPaintElement()
        elements += ScaledRectElement()
        elements += ScaledRectPivotElement()
        elements += ScaledCircleElement()
        elements += BitmapElement()
        elements += PathLineElement()
        elements += RectOutlineElement()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        choreographer.removeFrameCallback(frameCallback)
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth - paddingLeft - paddingRight
        val itemsPerLine = ((width + ELEMENT_MARGIN) / (ELEMENT_SIZE + ELEMENT_MARGIN)).toInt()
        val height = (ceil(elements.size / itemsPerLine.toFloat()) * (ELEMENT_SIZE + ELEMENT_MARGIN) - ELEMENT_MARGIN + 0.5f).toInt()

        setMeasuredDimension(measuredWidth, height)
    }

    override fun onDraw(canvas: Canvas) {
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val right = (width - paddingRight).toFloat()
        val bottom = (height - paddingBottom).toFloat()

        var x = left
        var y = top

        for (element in elements) {
            if (x + ELEMENT_SIZE > right) {
                x = left
                y += ELEMENT_SIZE + ELEMENT_MARGIN

                if ((y + ELEMENT_SIZE).toInt() > bottom)
                    break
            }

            bounds.set(x, y, x + ELEMENT_SIZE, y + ELEMENT_SIZE)
            element.onDraw(canvas, paint, bounds)

            x += ELEMENT_SIZE + ELEMENT_MARGIN
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {

        private val viewRect = Rect()
        private val rootRect = Rect()

        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)

            if (visibility != VISIBLE)
                return

            getGlobalVisibleRect(viewRect)
            rootView.getGlobalVisibleRect(rootRect)

            if (!Rect.intersects(viewRect, rootRect))
                return

            invalidate()
        }
    }

    private interface Element {
        fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF)
    }

    private class FixedArcSweepElement(private val useCenter: Boolean) : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            val angle = (System.currentTimeMillis() / 10 % 360).toFloat()

            val space = bounds.height() * 0.2f
            bounds.top += space
            bounds.bottom -= space

            paint.color = 0xff8fb9aa.toInt()
            canvas.drawArc(bounds, angle, 120f, useCenter, paint)
        }
    }

    private class VariableArcSweepAndAngleElement(private val useCenter: Boolean) : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            val anim = System.currentTimeMillis() / 5
            val angle = (System.currentTimeMillis() / 50 % 360).toFloat()
            val phase = (anim % 720).toFloat()
            var sweep = (anim % 360).toFloat()

            if (phase > 360f)
                sweep = 360 - sweep

            val space = bounds.width() * 0.2f
            bounds.left += space
            bounds.right -= space

            paint.color = 0xfff2d096.toInt()
            canvas.drawArc(bounds, angle, sweep, useCenter, paint)
        }
    }

    private class DrawPathElement : Element {

        private val path = Path()

        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            val width = bounds.width()
            val height = bounds.height()

            val left = width * 0.4f / 2f
            val top = height * 0.3f / 2f
            val right = left + width * 0.6f
            val bottom = top + height * 0.7f

            val angle = (System.currentTimeMillis() / 15 % 360).toFloat()
            val pivot = PointF(width / 2f, height / 2f)

            val a = PointF(left, top).rotate(angle, pivot).add(bounds.left, bounds.top)
            val b = PointF(left, bottom).rotate(angle, pivot).add(bounds.left, bounds.top)
            val c = PointF(right, bottom).rotate(angle, pivot).add(bounds.left, bounds.top)
            val d = PointF(right, top).rotate(angle, pivot).add(bounds.left, bounds.top)

            path.reset()
            path.moveTo(a.x, a.y)
            path.lineTo(b.x, b.y)
            path.lineTo(c.x, c.y)
            path.lineTo(d.x, d.y)
            path.close()

            paint.color = 0xffb2e7e8.toInt()
            canvas.drawPath(path, paint)
        }
    }

    private class ColorElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            canvas.save()
            canvas.clipRect(bounds)
            canvas.drawColor(0xff304d63.toInt())
            canvas.restore()
        }
    }

    private class TextElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = dpToPxF(12f)
            paint.color = 0xff304d63.toInt()

            val x = bounds.left + bounds.width() / 2f
            val y = bounds.top + bounds.height() / 2f - (paint.descent() - paint.ascent()) / 2f - paint.ascent()

            canvas.drawText("Text", x, y, paint)
        }
    }

    private class RgbElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            canvas.save()
            canvas.clipRect(bounds)
            canvas.drawRGB(200, 150, 60)
            canvas.restore()
        }
    }

    private class DrawPaintElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            canvas.save()
            canvas.clipRect(bounds)
            paint.shader = LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom, 0xff304d63.toInt(), 0xffed8975.toInt(), Shader.TileMode.REPEAT)
            canvas.drawPaint(paint)
            paint.shader = null
            canvas.restore()
        }
    }

    private class ScaledRectElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            paint.color = 0xff304d63.toInt()
            canvas.drawRect(bounds, paint)

            canvas.save()
            canvas.scale(0.5f, 0.5f)

            val width = bounds.width()
            val height = bounds.height()

            bounds.left *= 2f
            bounds.top *= 2f
            bounds.right = bounds.left + width
            bounds.bottom = bounds.top + height

            paint.color = 0xffed8975.toInt()
            canvas.translate(width / 2f, height / 2f)
            canvas.drawRect(bounds, paint)

            canvas.restore()
        }
    }

    private class ScaledRectPivotElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            val width = bounds.width()
            val height = bounds.height()

            paint.color = 0xff8fb9aa.toInt()
            canvas.drawRect(bounds, paint)

            canvas.save()
            canvas.scale(0.5f, 0.5f, width, height)

            bounds.left *= 2f
            bounds.top *= 2f
            bounds.right = bounds.left + width
            bounds.bottom = bounds.top + height

            paint.color = 0xfff2d096.toInt()
            canvas.translate(-width / 2f, -height / 2f)
            canvas.drawRect(bounds, paint)
            canvas.restore()
        }
    }

    private class ScaledCircleElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            val width = bounds.width()
            val height = bounds.height()

            val cx = bounds.left + width / 2f
            val cy = bounds.top + height / 2f
            val radius = min(width, height) / 2f

            paint.color = 0xffed8975.toInt()
            canvas.drawCircle(cx, cy, radius, paint)

            canvas.save()
            canvas.scale(2f, 2f)
            canvas.scale(0.25f, 0.25f)

            paint.color = 0xff304d63.toInt()
            canvas.drawCircle(cx * 2f, cy * 2f, radius, paint)

            canvas.restore()
        }
    }

    private inner class BitmapElement : Element {

        private val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_bitmap_sample_5)

        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            canvas.save()
            canvas.clipRect(bounds)
            canvas.drawBitmap(bitmap, null, bounds, null)
            canvas.restore()
        }
    }

    private class PathLineElement : Element {

        private val strokeWidth = dpToPxF(5f)
        private val cornerPathEffect = CornerPathEffect(strokeWidth)
        private val points = arrayOf(PointF(0f, 0.5f), PointF(0.1f, 0.5f), PointF(0.33f, 0.2f), PointF(0.66f, 0.8f), PointF(0.9f, 0.5f), PointF(1f, 0.5f))
        private val shader = LinearGradient(0f, 0f, ELEMENT_SIZE, ELEMENT_SIZE, intArrayOf(0xff8fb9aa.toInt(), 0xffed8975.toInt()), floatArrayOf(0f, 1f), Shader.TileMode.MIRROR)
        private val path = Path()

        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            val width = bounds.width()
            val height = bounds.height()

            path.reset()
            path.moveTo(bounds.left + points[0].x * width, bounds.top + points[0].y * height)

            for (i in 1 until points.size)
                path.lineTo(bounds.left + points[i].x * width, bounds.top + points[i].y * height)

            paint.color = 0xff000000.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.shader = shader
            paint.strokeCap = Paint.Cap.ROUND
            paint.pathEffect = cornerPathEffect

            canvas.drawPath(path, paint)

            paint.style = Paint.Style.FILL
            paint.shader = null
            paint.strokeCap = Paint.Cap.BUTT
            paint.pathEffect = null
        }
    }

    private class RectOutlineElement : Element {
        override fun onDraw(canvas: Canvas, paint: Paint, bounds: RectF) {
            paint.color = 0xff304d63.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dpToPxF(10f)

            val width = bounds.width()
            val height = bounds.height()

            bounds.left += width * 0.3f
            bounds.top += height * 0.3f
            bounds.right -= width * 0.3f
            bounds.bottom -= height * 0.3f

            canvas.drawRect(bounds, paint)

            paint.style = Paint.Style.FILL
        }
    }

    companion object {
        private val ELEMENT_MARGIN = dpToPxF(10f)
        private val ELEMENT_SIZE = dpToPxF(50f)
    }
}
