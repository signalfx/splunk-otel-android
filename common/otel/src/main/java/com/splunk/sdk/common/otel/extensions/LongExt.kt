package com.splunk.sdk.common.otel.extensions

import java.time.Instant

fun Long.toInstant(): Instant {
    return Instant.ofEpochMilli(this)
}
