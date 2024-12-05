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

package com.smartlook.app.extension

import android.graphics.PointF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ZERO = PointF(0f, 0f)

fun PointF.rotate(angle: Float, pivot: PointF = ZERO): PointF {
    val angleRad = angle * PI.toFloat() / 180f

    val cosTheta = cos(angleRad)
    val sinTheta = sin(angleRad)

    val tempX = cosTheta * (x - pivot.x) - sinTheta * (y - pivot.y) + pivot.x
    val tempY = sinTheta * (x - pivot.x) + cosTheta * (y - pivot.y) + pivot.y

    x = tempX
    y = tempY

    return this
}

fun PointF.add(offsetX: Float, offsetY: Float): PointF {
    x += offsetX
    y += offsetY

    return this
}
