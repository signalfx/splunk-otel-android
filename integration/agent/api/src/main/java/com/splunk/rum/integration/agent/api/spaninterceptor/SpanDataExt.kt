package com.splunk.rum.integration.agent.api.spaninterceptor

import io.opentelemetry.sdk.trace.data.SpanData

/**
 * Converts this [SpanData] instance into a [MutableSpanData].
 *
 * This is a convenience method for clients working with the Agent's
 * span interception feature, where mutating spans may be necessary for filtering,
 * enrichment, or transformation.
 *
 * @receiver The original [SpanData] to be wrapped in a mutable representation.
 * @return A [MutableSpanData] instance that reflects the original [SpanData]'s values.
 */
fun SpanData.toMutableSpanData(): MutableSpanData = MutableSpanData(this)