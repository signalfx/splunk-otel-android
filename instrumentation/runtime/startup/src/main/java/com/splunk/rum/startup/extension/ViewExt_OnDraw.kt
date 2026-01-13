/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.startup.extension

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver

// FIXME Compiler error. Use com.splunk.android.common.utils.extensions.doOnDraw once the issue is fixed.
internal inline fun View.doOnDraw(crossinline action: () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        doOnDraw29(action)
    else
        doOnDraw20(action)
}

private inline fun View.doOnDraw20(crossinline action: () -> Unit) {
    val onPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            action()

            if (viewTreeObserver.isAlive)
                rootView.viewTreeObserver.removeOnPreDrawListener(this)

            return true
        }
    }

    viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
}

private inline fun View.doOnDraw29(crossinline action: () -> Unit) {
    var pendingRemove = false

    val onDrawListener = ViewTreeObserver.OnDrawListener {
        pendingRemove = true
        action()
    }

    val onPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (pendingRemove && viewTreeObserver.isAlive) {
                rootView.viewTreeObserver.removeOnDrawListener(onDrawListener)
                rootView.viewTreeObserver.removeOnPreDrawListener(this)
            }

            return true
        }
    }

    viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
    viewTreeObserver.addOnDrawListener(onDrawListener)
}
