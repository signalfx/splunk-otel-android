package com.splunk.sdk.common.utils.extensions

import android.app.Activity
import android.app.Application
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup

val View.activity: Activity?
    get() {
        var context = context

        while (context != null) {
            if (context is Activity)
                return context

            if (context is ContextWrapper && context !is Application)
                context = context.baseContext
            else
                break
        }

        return if (this is ViewGroup) getChildAt(0)?.activity else null
    }
