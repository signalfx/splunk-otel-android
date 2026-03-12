package com.splunk.rum.utils.extensions

import android.util.Base64
import android.util.Base64.encodeToString

fun String.toBase64(): String = encodeToString(this.toByteArray(), Base64.NO_WRAP)
