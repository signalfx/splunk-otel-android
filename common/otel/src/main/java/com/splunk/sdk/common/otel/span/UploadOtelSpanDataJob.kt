/*
 * Copyright 2024 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.sdk.common.otel.span

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import com.cisco.android.common.http.HttpClient
import com.cisco.android.common.http.model.Header
import com.cisco.android.common.http.model.Response
import com.cisco.android.common.job.JobIdStorage
import com.cisco.android.common.logger.Logger
import com.splunk.sdk.common.storage.AgentStorage
import java.net.UnknownHostException

@SuppressLint("NewApi")
internal class UploadOtelSpanDataJob : JobService() {

    private val storage by lazy { AgentStorage.attach(application) }
    private val jobIdStorage by lazy { JobIdStorage.init(application) }
    private val httpClient by lazy { HttpClient() }

    override fun onStopJob(params: JobParameters?): Boolean {
        Logger.d(TAG, "onStopJob()")
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Logger.d(TAG, "onStartJob()")
        startUpload(params)
        return true
    }

    private fun startUpload(params: JobParameters?) {
        params?.extras?.getString(DATA_SERIALIZE_KEY)?.let { id ->
            Logger.d(TAG, "startUpload() id: $id")

            val url = storage.readBaseUrl()

            if (url == null) {
                Logger.d(TAG, "startUpload() url is not valid")
                jobFinished(params, false)
                return
            }

            val data = storage.readOtelSpanData(id)

            if (data == null) {
                Logger.d(TAG, "startUpload() data is not valid")
                jobFinished(params, false)
                return
            }

            httpClient.makePostRequest(
                url = url,
                queries = emptyList(),
                headers = listOf(Header("Content-Type", "application/x-protobuf")),
                body = data,
                callback = object : HttpClient.Callback {
                    override fun onSuccess(response: Response) {
                        Logger.d(TAG, "startUpload() onSuccess: response=$response, code=${response.code}, body=${response.body.toString(Charsets.UTF_8)}")
                        deleteData(id)
                        if (response.isSuccessful) {
                            jobFinished(params, false)
                        } else {
                            jobFinished(params, false)
                        }
                    }

                    override fun onFailed(e: Exception) {
                        Logger.d(TAG, "startUpload() onFailed: e=$e")
                        when (e) {
                            is UnknownHostException -> jobFinished(params, true)
                            else -> {
                                deleteData(id)
                                jobFinished(params, false)
                            }
                        }
                    }
                }
            )
        } ?: jobFinished(params, false)
    }

    private fun deleteData(id: String) {
        jobIdStorage.delete(id)
        storage.deleteOtelSpanData(id)
    }

    companion object {
        private const val TAG = "UploadOtelSpanDataJob"
        private const val DATA_SERIALIZE_KEY = "DATA"

        fun createJobInfoBuilder(
            context: Context,
            jobId: Int,
            id: String
        ): JobInfo.Builder = JobInfo.Builder(jobId, ComponentName(context, UploadOtelSpanDataJob::class.java))
            .setExtras(PersistableBundle().apply { putString(DATA_SERIALIZE_KEY, id) })
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresCharging(false)
    }
}
