package com.smartlook.sdk.common.utils.extensions

/**
 * Function is using indices instead of iterator.
 */
inline fun <T> List<T>.forEachFast(crossinline consumer: (T) -> Unit) {
    for (i in indices)
        consumer(get(i))
}
