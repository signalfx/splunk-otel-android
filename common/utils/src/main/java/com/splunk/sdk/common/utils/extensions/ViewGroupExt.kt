package com.splunk.sdk.common.utils.extensions

import android.view.View
import android.view.ViewGroup

fun ViewGroup.iterator(): Iterator<View> {
    return object : Iterator<View> {
        private var index = 0

        override fun hasNext(): Boolean = index < childCount
        override fun next(): View = getChildAt(index++) ?: throw IndexOutOfBoundsException()
    }
}
