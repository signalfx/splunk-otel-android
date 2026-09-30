/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.agent.common.otel.span

import io.opentelemetry.sdk.trace.data.SpanData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Coordinates exact local-persistence acknowledgement for a small set of spans that must survive
 * imminent process termination.
 *
 * This is a library integration API. Callers register a span ID before ending the span, request a
 * tracer-provider flush, and wait for this coordinator. A successful result means a batch
 * containing that exact span ID was written to the SDK's durable span store.
 */
object DurableSpanPersistenceCoordinator {

    private class PendingPersistence {
        val result = AtomicReference<Boolean?>(null)
        val latch = CountDownLatch(1)

        fun complete(success: Boolean) {
            if (result.compareAndSet(null, success)) {
                latch.countDown()
            }
        }
    }

    private val pending = ConcurrentHashMap<String, PendingPersistence>()

    /** Registers [spanId] before the corresponding span is ended. */
    @JvmStatic
    fun register(spanId: String): Boolean {
        if (spanId.isBlank()) return false
        return pending.putIfAbsent(spanId, PendingPersistence()) == null
    }

    /**
     * Waits at most [timeoutMillis] for the registered [spanId] to reach durable local storage.
     * The registration is consumed by this call, regardless of the result.
     */
    @JvmStatic
    fun await(spanId: String, timeoutMillis: Long): Boolean {
        val entry = pending[spanId] ?: return false
        val boundedTimeout = timeoutMillis.coerceAtLeast(0)
        return try {
            entry.latch.await(boundedTimeout, TimeUnit.MILLISECONDS) && entry.result.get() == true
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } finally {
            pending.remove(spanId, entry)
        }
    }

    /** Cancels and consumes a registration that will no longer be awaited. */
    @JvmStatic
    fun cancel(spanId: String) {
        pending.remove(spanId)?.complete(false)
    }

    internal fun complete(spans: Collection<SpanData>, success: Boolean) {
        spans.forEach { span ->
            pending[span.spanContext.spanId]?.complete(success)
        }
    }

    internal fun resetForTest() {
        pending.values.forEach { it.complete(false) }
        pending.clear()
    }
}
