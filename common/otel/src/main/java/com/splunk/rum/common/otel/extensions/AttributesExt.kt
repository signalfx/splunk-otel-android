package com.splunk.rum.common.otel.extensions

import io.opentelemetry.api.common.Attributes

/**
 * Converts the [Attributes] to a string in the format of "[key1=value1, key2=value2, ...]".
 */
fun Attributes.toSplunkString(): String {
    val builder = StringBuilder()
    builder.append('[')

    val iterator = asMap().entries.iterator()
    if (iterator.hasNext()) {
        val first = iterator.next()
        builder.append(first.key)
            .append('=')
            .append(first.value)

        while (iterator.hasNext()) {
            val entry = iterator.next()
            builder.append(", ")
                .append(entry.key)
                .append('=')
                .append(entry.value)
        }
    }

    builder.append(']')
    return builder.toString()
}
