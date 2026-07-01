/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.splunk.rum.instrumentation.okhttp3.common.internal

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder
import okhttp3.Interceptor
import okhttp3.Response

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
class OkHttpClientInstrumenterBuilderFactory private constructor() {

    companion object {
        private const val INSTRUMENTATION_NAME = "com.splunk.rum.okhttp-3.0"

        @JvmStatic
        fun create(openTelemetry: OpenTelemetry): DefaultHttpClientInstrumenterBuilder<Interceptor.Chain, Response> =
            DefaultHttpClientInstrumenterBuilder.create(
                INSTRUMENTATION_NAME,
                openTelemetry,
                OkHttpAttributesGetter.INSTANCE
            ).addAttributesExtractor(OkHttp3AdditionalAttributesExtractor())
    }
}
