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

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.animation.BounceInterpolator
import androidx.core.animation.addListener

class TomasTextureView(context: Context, attrs: AttributeSet? = null) : TextureView(context, attrs) {

    private val paint = Paint()

    private var surface: Surface? = null

    private val animator = ValueAnimator.ofFloat(0f, 1f)
    private var isCanceled = false
    private val rectTemp = Rect()
    private var transitionX = 0

    val elements = ArrayList<TomasElement>()

    var listener: Listener? = null

    init {
        surfaceTextureListener = MySurfaceTextureListener()
        // TODO out of scope for GA
//        elements += TomasElement.Rectangle(Rect(0f.px, 0f.px, 150f.px, 20f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(0f.px, 25f.px, 125f.px, 45f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(0f.px, 50f.px, 95f.px, 120f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(100f.px, 50f.px, 175f.px, 70f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(100f.px, 75f.px, 150f.px, 95f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(100f.px, 100f.px, 130f.px, 120f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(0f.px, 125f.px, 150f.px, 145f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(0f.px, 150f.px, 185f.px, 170f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(0f.px, 175f.px, 190f.px, 195f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(0f.px, 200f.px, 150f.px, 220f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Rectangle(Rect(0f.px, 225f.px, 185f.px, 245f.px), 0xffff00ff.toInt(), true)
//        elements += TomasElement.Circle(80f.px, 70f.px, 30f.px, 0xff0000aa.toInt(), false)
//        elements += TomasElement.Rectangle(Rect(25f.px, 130f.px, 125f.px, 230f.px), 0xff0000ff.toInt(), false)
//        elements += TomasElement.GradientRectangle(Rect(25f.px, 130f.px, 125f.px, 230f.px), 0xff0000ff.toInt(), 0xffff0000.toInt(), false)
//
//        animator.addUpdateListener {
//            val fraction = it.animatedValue as Float
//
//            transitionX = if (animator.currentPlayTime > 1000L)
//                ((1f - INTERPOLATOR.getInterpolation(1f - fraction)) * 50f.px).toInt()
//            else
//                (INTERPOLATOR.getInterpolation(fraction) * 50f.px).toInt()
//
//            redraw()
//        }

        animator.addListener(
            onStart = {
                listener?.onTransitionChanged(true)
            },
            onEnd = {
                listener?.onTransitionChanged(false)

                if (isAttachedToWindow && !isCanceled)
                    animator.start()
            }
        )

        animator.repeatMode = ValueAnimator.REVERSE
        animator.startDelay = 5000L
        animator.duration = 1000L
        animator.repeatCount = 1
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isCanceled = false
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        isCanceled = true
        animator.cancel()
    }

    private fun redraw() {
        val surface = surface ?: return
        val canvas = surface.lockCanvas(null)

        canvas.drawColor(Color.WHITE)

        for (element in elements)
            when (element) {
                is TomasElement.Rectangle -> {
                    paint.color = element.color
                    rectTemp.set(element.rect)
                    rectTemp.offset(transitionX, 0)
                    canvas.drawRect(rectTemp, paint)
                }
                is TomasElement.Circle -> {
                    paint.color = element.color
                    canvas.drawCircle(element.x.toFloat() + transitionX, element.y.toFloat(), element.radius.toFloat(), paint)
                }
                is TomasElement.GradientRectangle -> {
                    paint.shader = LinearGradient(element.rect.left.toFloat(), element.rect.top.toFloat(), element.rect.right.toFloat(), element.rect.bottom.toFloat(), element.topLeftColor, element.bottomRightColor, Shader.TileMode.CLAMP)
                    rectTemp.set(element.rect)
                    rectTemp.offset(transitionX, 0)
                    canvas.drawRect(rectTemp, paint)
                    paint.shader = null
                }
            }

        surface.unlockCanvasAndPost(canvas)
    }

    private inner class MySurfaceTextureListener : SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            surface = Surface(surfaceTexture)
            redraw()
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            surface = null
            return true
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    interface Listener {
        fun onTransitionChanged(isRunning: Boolean)
    }

    private companion object {
        val INTERPOLATOR = BounceInterpolator()
    }
}
