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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatImageView
import com.smartlook.app.R
import com.smartlook.sdk.common.utils.dpToPxF

class HardwareBitmapView(context: Context, attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {

    private val paint = Paint()
    private val rect = Rect()
    private val bitmap: Bitmap?

    init {
        bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !isInEditMode) {
            val source = ImageDecoder.createSource(resources, R.drawable.ic_bitmap_big)
            ImageDecoder.decodeBitmap(source, OnHeaderDecodedListener())
        } else
            null

        if (bitmap != null) {
            background = HwBitmapDrawable(bitmap)
            setImageBitmap(bitmap)
        }

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = dpToPxF(15f)
        paint.color = Color.RED
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (bitmap != null) {
            rect.right = width
            rect.bottom = height
            canvas.drawBitmap(bitmap, null, rect, null)
        } else
            canvas.drawText("Android 9+", width / 2f, (height + paint.descent() - paint.ascent()) / 2f, paint)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private class OnHeaderDecodedListener : ImageDecoder.OnHeaderDecodedListener {
        override fun onHeaderDecoded(decoder: ImageDecoder, info: ImageDecoder.ImageInfo, source: ImageDecoder.Source) {
            decoder.allocator = ImageDecoder.ALLOCATOR_HARDWARE
        }
    }

    private class HwBitmapDrawable(private val bitmap: Bitmap) : Drawable() {

        private val dstRect = Rect()

        override fun draw(canvas: Canvas) {
            dstRect.right = intrinsicWidth
            dstRect.bottom = intrinsicHeight

            canvas.drawBitmap(bitmap, null, dstRect, null)
        }

        override fun setAlpha(alpha: Int) {}

        override fun setColorFilter(colorFilter: ColorFilter?) {}

        @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat"))
        override fun getOpacity(): Int {
            return PixelFormat.TRANSPARENT
        }
    }
}
