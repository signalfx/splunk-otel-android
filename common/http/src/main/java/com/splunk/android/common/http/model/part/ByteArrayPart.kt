package com.splunk.android.common.http.model.part

import androidx.annotation.WorkerThread

class ByteArrayPart(
    override val name: String,
    override val contentType: String,
    override val contentEncoding: String? = null,
    val bytes: ByteArray
) : Part {

    @WorkerThread
    override fun getLength(): Long {
        return bytes.size.toLong()
    }
}
