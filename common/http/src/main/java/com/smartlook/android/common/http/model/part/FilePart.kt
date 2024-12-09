package com.smartlook.android.common.http.model.part

import androidx.annotation.WorkerThread
import java.io.File

data class FilePart(
    override val name: String,
    override val contentType: String,
    override val contentEncoding: String? = null,
    val file: File,
) : Part {

    @WorkerThread
    override fun getLength(): Long {
        return file.length()
    }
}
