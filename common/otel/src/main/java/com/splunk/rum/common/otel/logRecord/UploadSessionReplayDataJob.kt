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

package com.splunk.rum.common.otel.logRecord

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
import com.splunk.android.common.utils.extensions.safeSubmit
import com.splunk.android.common.utils.thread.NamedThreadFactory
import com.splunk.rum.common.otel.http.AuthHeaderBuilder
import com.splunk.rum.common.storage.AgentStorage
import java.net.UnknownHostException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class UploadSessionReplayDataJob : JobService() {

    private val storage by lazy { AgentStorage.attach(application) }
    private val jobIdStorage by lazy { JobIdStorage.init(application, isEncrypted = false) }
    private val httpClient by lazy { HttpClient() }

    private val executor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor(NamedThreadFactory("uploadSessionReplayExecutor"))
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Logger.d(TAG, "onStopJob()")
        executor.shutdownNow()
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Logger.d(TAG, "onStartJob()")
        startUpload(params)
        return true
    }

    private fun startUpload(params: JobParameters?) {
        if (params == null) {
            jobFinished(params, false)
            return
        }

        val id = params.extras?.getString(DATA_SERIALIZE_KEY)

        if (id == null) {
            jobFinished(params, false)
            return
        }

        Logger.d(TAG) { "startUpload() id: $id" }
        executor.safeSubmit {
            val url = storage.readLogsBaseUrl()

            if (url == null) {
                Logger.d(TAG, "startUpload() url is not valid")
                jobFinished(params, false)
                return@safeSubmit
            }

            val dataFile = storage.getOtelSessionReplayDataFile(id)

            if (dataFile == null) {
                Logger.d(TAG, "startUpload() session replay file is not present")
                jobFinished(params, false)
                return@safeSubmit
            }

            val headers = AuthHeaderBuilder.buildHeaders(storage, TAG)

            httpClient.makePostRequest(
                url = url,
                queries = emptyList(),
                headers = headers,
                body = dataFile,
                callback = object : HttpClient.Callback {
                    override fun onSuccess(response: Response) {
                        Logger.d(TAG) {
                            "startUpload() onSuccess: response=$response, code=${response.code}," +
                                " body=${response.body.toString(Charsets.UTF_8)}"
                        }
                        deleteData(id)
                        jobFinished(params, false)
                    }

                    override fun onFailed(e: Exception) {
                        Logger.d(TAG, "startUpload() onFailed", e)

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
        }
    }

    private fun deleteData(id: String) {
        jobIdStorage.delete(id)
        storage.deleteOtelSessionReplayData(id)
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "UploadSessionReplayDataJob"
        private const val DATA_SERIALIZE_KEY = "DATA"

        private const val INITIAL_BACKOFF = 60 * 1000L

        fun createJobInfoBuilder(context: Context, jobId: Int, id: String): JobInfo.Builder =
            JobInfo.Builder(jobId, ComponentName(context, UploadSessionReplayDataJob::class.java))
                .setExtras(PersistableBundle().apply { putString(DATA_SERIALIZE_KEY, id) })
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setRequiresCharging(false)
    }
}
