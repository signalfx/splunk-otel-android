package com.splunk.rum.common.otel.extensions

import io.opentelemetry.api.common.Attributes

/**
 * Appends the given [attributes] to this [StringBuilder] in a human-readable format.
 * The format is: [key1=value1, key2=value2, ...].
 *
 * @param attributes The attributes to append.
 */
fun StringBuilder.appendAttributes(attributes: Attributes) {
    append('[')
    val iterator = attributes.asMap().entries.iterator()
    if (iterator.hasNext()) {
        val first = iterator.next()
        append(first.key).append('=').append(first.value)

        while (iterator.hasNext()) {
            val entry = iterator.next()
            append(", ")
                .append(entry.key)
                .append('=')
                .append(entry.value)
        }
    }

    append(']')
}
