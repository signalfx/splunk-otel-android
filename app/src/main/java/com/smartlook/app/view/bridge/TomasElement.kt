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

import android.graphics.Rect

sealed interface TomasElement {

    val isSensitive: Boolean

    data class Rectangle(val rect: Rect, val color: Int, override val isSensitive: Boolean) : TomasElement

    data class GradientRectangle(val rect: Rect, val topLeftColor: Int, val bottomRightColor: Int, override val isSensitive: Boolean) : TomasElement

    data class Circle(val x: Int, val y: Int, val radius: Int, val color: Int, override val isSensitive: Boolean) : TomasElement
}
