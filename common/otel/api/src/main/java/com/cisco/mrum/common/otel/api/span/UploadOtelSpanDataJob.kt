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

package com.cisco.mrum.common.otel.api.span

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.cisco.mrum.common.otel.internal.storage.OtelStorage
import com.smartlook.android.common.http.HttpClient
import com.smartlook.android.common.http.model.Header
import com.smartlook.android.common.http.model.Response
import com.smartlook.sdk.common.job.JobIdStorage
import com.smartlook.sdk.common.logger.Logger
import com.smartlook.sdk.common.storage.Storage
import com.smartlook.sdk.log.LogAspect
import java.net.UnknownHostException

@SuppressLint("NewApi")
internal class UploadOtelSpanDataJob : JobService() {

    private val storage by lazy { Storage.attach(application) }
    private val jobIdStorage by lazy { JobIdStorage.init(storage) }
    private val httpClient by lazy { HttpClient() }
    private val otelStorage by lazy { OtelStorage.obtainInstance(storage.preferences) }

    override fun onStopJob(params: JobParameters?): Boolean {
        Logger.privateD(LogAspect.JOB, TAG, { "onStopJob()" })
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Logger.privateD(LogAspect.JOB, TAG, { "onStartJob()" })
        startUpload(params)
        return true
    }

    private fun startUpload(params: JobParameters?) {
        params?.extras?.getString(DATA_SERIALIZE_KEY)?.let { id ->
            Logger.privateD(LogAspect.JOB, TAG, { "startUpload() id: $id" })

            val url = otelStorage.readBaseUrl()?.let { "$it/eum/v1/traces" }

            if (url == null) {
                Logger.privateD(LogAspect.JOB, TAG, { "startUpload() url is not valid" })
                jobFinished(params, false)
                return
            }

            httpClient.makePostRequest(
                url = url,
                queries = emptyList(),
                headers = listOf(Header("Content-Type", "application/x-protobuf")),
                body = storage.getOtelSpanDataFile(id),
                callback = object : HttpClient.Callback {
                    override fun onSuccess(response: Response) {
                        Logger.privateD(LogAspect.JOB, TAG, { "startUpload() onSuccess: response=$response, code=${response.code}, body=${response.body.toString(Charsets.UTF_8)}" })
                        deleteData(id)
                        if (response.isSuccessful) {
                            jobFinished(params, false)
                        } else {
                            // TODO: Maybe there is a case when we would like to reschedule.
                            jobFinished(params, false)
                        }
                    }

                    override fun onFailed(e: Exception) {
                        Logger.privateD(LogAspect.JOB, TAG, { "startUpload() onFailed: e=$e" })
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
        storage.getOtelSpanDataFile(id).delete()
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
