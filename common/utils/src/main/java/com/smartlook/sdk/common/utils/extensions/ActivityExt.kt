package com.smartlook.sdk.common.utils.extensions

import android.app.Activity
import android.view.ViewGroup

val Activity.contentView: ViewGroup?
    get() = findViewById(android.R.id.content)
