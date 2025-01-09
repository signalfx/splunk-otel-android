package com.splunk.sdk.common.utils.extensions

operator fun <E> MutableCollection<E>.plusAssign(element: E?) {
    add(element ?: return)
}

operator fun <E> MutableCollection<E>.plusAssign(list: List<E>?) {
    addAll(list ?: return)
}

operator fun <E> MutableCollection<E>.minusAssign(element: E?) {
    remove(element ?: return)
}
