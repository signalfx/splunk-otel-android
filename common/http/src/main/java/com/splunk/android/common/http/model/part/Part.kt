package com.splunk.android.common.http.model.part

import androidx.annotation.WorkerThread

sealed interface Part {

    val name: String
    val contentType: String
    val contentEncoding: String?

    @WorkerThread
    fun getLength(): Long
}
