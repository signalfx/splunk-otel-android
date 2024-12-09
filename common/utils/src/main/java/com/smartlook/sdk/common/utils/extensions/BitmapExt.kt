package com.smartlook.sdk.common.utils.extensions

import android.graphics.Bitmap

operator fun Bitmap.get(x: Int, y: Int): Int {
    return getPixel(x, y)
}
