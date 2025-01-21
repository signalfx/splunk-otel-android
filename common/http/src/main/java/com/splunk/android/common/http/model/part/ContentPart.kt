package com.splunk.android.common.http.model.part

import androidx.annotation.WorkerThread
import java.io.OutputStream

abstract class ContentPart(
    override val name: String,
    override val contentType: String,
    override val contentEncoding: String? = null
) : Part {

    @WorkerThread
    abstract fun copyInto(stream: OutputStream)
}
