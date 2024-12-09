package com.smartlook.android.common.http.model

class Response internal constructor(
    val code: Int,
    val headers: List<Header>,
    val body: ByteArray
) {

    val isSuccessful: Boolean
        get() = code in 200..299
}
