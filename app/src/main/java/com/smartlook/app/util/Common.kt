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

package com.smartlook.app.util

import android.graphics.Color

fun randomColor(): Int {
    val hue = Math.random().toFloat() * 360f
    val saturation = (Math.random().toFloat() * 2000f + 1000f) / 10000f
    val luminance = 0.9f
    return Color.HSVToColor(floatArrayOf(hue, saturation, luminance))
}
