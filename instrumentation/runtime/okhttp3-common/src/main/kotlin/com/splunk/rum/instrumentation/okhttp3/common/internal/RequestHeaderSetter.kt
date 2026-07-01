/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.rum.instrumentation.okhttp3.common.internal

import io.opentelemetry.context.propagation.TextMapSetter
import okhttp3.Request

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 *
 * Helper class to inject span context into request headers.
 */
internal enum class RequestHeaderSetter : TextMapSetter<Request.Builder> {
    INSTANCE;

    override fun set(carrier: Request.Builder?, key: String, value: String) {
        if (carrier == null) {
            return
        }
        carrier.header(key, value)
    }
}
