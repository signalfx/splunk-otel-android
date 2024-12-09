package com.smartlook.sdk.common.utils.extensions

operator fun StringBuilder.plusAssign(value: Char) {
    append(value)
}

operator fun StringBuilder.plusAssign(value: String) {
    append(value)
}
