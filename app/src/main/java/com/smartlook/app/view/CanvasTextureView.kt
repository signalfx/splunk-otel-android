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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.SurfaceTexture
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView

// FIXME Animations

class CanvasTextureView(context: Context, attrs: AttributeSet? = null) : TextureView(context, attrs) {

    init {
        surfaceTextureListener = MySurfaceTextureListener()
    }

    private inner class MySurfaceTextureListener : SurfaceTextureListener {

        private var surface: Surface? = null

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            val localSurface = Surface(surface)
            this.surface = localSurface

            draw(localSurface, width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

        private fun draw(surface: Surface, width: Int, height: Int) {
            val canvas = surface.lockCanvas(null)
            val paint = Paint()
            val path = Path()

            canvas.drawColor(0xffcccccc.toInt())

            paint.color = Color.BLUE
            canvas.drawRect(width * 0.25f, height * 0.25f, width * 0.75f, height * 0.75f, paint)

            path.moveTo(width * 0.5f, height * 0.2f)
            path.lineTo(width * 0.15f, height * 0.65f)
            path.lineTo(width * 0.85f, height * 0.65f)
            path.close()
            paint.color = Color.RED

            canvas.save()
            canvas.rotate(-0.09f * SystemClock.uptimeMillis() % 4000L, width / 2f, height / 2f)
            canvas.drawPath(path, paint)
            canvas.restore()

            surface.unlockCanvasAndPost(canvas)
        }
    }
}
