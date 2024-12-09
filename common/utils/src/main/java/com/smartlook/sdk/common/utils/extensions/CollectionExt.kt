package com.smartlook.sdk.common.utils.extensions

/**
 * Function is using indices instead of iterator if possible.
 */
inline fun <T> Collection<T>.forEachFast(crossinline consumer: (T) -> Unit) {
    if (this is List)
        forEachFast(consumer)
    else
        forEach(consumer)
}

operator fun <T> Collection<T>.contains(elements: Collection<T>): Boolean {
    return containsAll(elements)
}
