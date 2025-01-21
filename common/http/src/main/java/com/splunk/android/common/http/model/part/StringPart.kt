package com.splunk.android.common.http.model.part

import androidx.annotation.WorkerThread

data class StringPart(
    override val name: String,
    override val contentType: String,
    val string: String
) : Part {

    override val contentEncoding: String? = null

    @WorkerThread
    override fun getLength(): Long {
        return string.length.toLong()
    }
}
