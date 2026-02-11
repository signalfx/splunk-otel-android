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

package com.splunk.rum.common.otel.span

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.splunk.android.common.http.HttpClient
import com.splunk.android.common.http.model.Response
import com.splunk.android.common.job.JobIdStorage
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.runOnBackgroundThread
import com.splunk.rum.common.otel.http.AuthHeaderBuilder
import com.splunk.rum.common.storage.AgentStorage
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean

internal class UploadOtelSpanDataJob : JobService() {

    private val storage by lazy { AgentStorage.attach(application) }
    private val jobIdStorage by lazy { JobIdStorage.init(application, isEncrypted = false) }
    private val httpClient by lazy { HttpClient() }

    private var thread: Thread? = null

    override fun onStopJob(params: JobParameters?): Boolean {
        Logger.d(TAG, "onStopJob()")
        thread?.interrupt()
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Logger.d(TAG, "onStartJob()")
        startUpload(params)
        return true
    }

    private fun startUpload(params: JobParameters?) {
        if (params == null) {
            return
        }

        val finished = AtomicBoolean(false)
        val id = params.extras?.getString(DATA_SERIALIZE_KEY)

        if (id == null) {
            finishOnce(finished, params, false)
            return
        }

        Logger.d(TAG, "startUpload() id: $id")
        thread = runOnBackgroundThread {
            val url = storage.readTracesBaseUrl()

            if (url == null) {
                Logger.d(TAG, "startUpload() url is not valid")
                finishOnce(finished, params, false)
                return@runOnBackgroundThread
            }

            val data = storage.readOtelSpanData(id)

            if (data == null) {
                Logger.d(TAG, "startUpload() data is not valid")
                finishOnce(finished, params, false)
                return@runOnBackgroundThread
            }

            val headers = AuthHeaderBuilder.buildHeaders(storage, TAG)

            httpClient.makePostRequest(
                url = url,
                queries = emptyList(),
                headers = headers,
                body = data,
                callback = object : HttpClient.Callback {
                    override fun onSuccess(response: Response) {
                        Logger.d(
                            TAG,
                            "startUpload() onSuccess: response=$response, code=${response.code}," +
                                " body=${response.body.toString(
                                    Charsets.UTF_8
                                )}"
                        )
                        deleteData(id)
                        finishOnce(finished, params, false)
                    }

                    override fun onFailed(e: Exception) {
                        Logger.d(TAG, "startUpload() onFailed: e=$e")
                        when (e) {
                            is UnknownHostException -> finishOnce(finished, params, true)
                            else -> {
                                deleteData(id)
                                finishOnce(finished, params, false)
                            }
                        }
                    }
                }
            )
        }
    }

    private fun deleteData(id: String) {
        jobIdStorage.delete(id)
        storage.deleteOtelSpanData(id)
    }

    private fun finishOnce(finished: AtomicBoolean, params: JobParameters, needsReschedule: Boolean) {
        if (finished.compareAndSet(false, true)) {
            jobFinished(params, needsReschedule)
        }
    }

    companion object {
        private const val TAG = "UploadOtelSpanDataJob"
        private const val DATA_SERIALIZE_KEY = "DATA"

        fun createJobInfoBuilder(context: Context, jobId: Int, id: String): JobInfo.Builder =
            JobInfo.Builder(jobId, ComponentName(context, UploadOtelSpanDataJob::class.java))
                .setExtras(PersistableBundle().apply { putString(DATA_SERIALIZE_KEY, id) })
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresCharging(false)
    }
}
