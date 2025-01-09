package com.splunk.sdk.common.storage.extensions

internal val Int.MB: Long
    get() = (this * 1024 * 1024).toLong()
