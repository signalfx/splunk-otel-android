/*
 * Copyright 2025 Splunk Inc.
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