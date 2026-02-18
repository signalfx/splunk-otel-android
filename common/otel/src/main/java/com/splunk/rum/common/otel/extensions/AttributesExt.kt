package com.splunk.rum.common.otel.extensions

import io.opentelemetry.api.common.Attributes

/**
 * Converts the [Attributes] to a string in the format of "[key1=value1, key2=value2, ...]".
 */
fun Attributes.toSplunkString() = buildString {
    append('[')

    val iterator = asMap().entries.iterator()
    if (iterator.hasNext()) {
        val first = iterator.next()

        append(first.key)
        append('=')
        append(first.value)

        while (iterator.hasNext()) {
            val entry = iterator.next()
            append(", ")
            append(entry.key)
            append('=')
            append(entry.value)
        }
    }

    append(']')
}
