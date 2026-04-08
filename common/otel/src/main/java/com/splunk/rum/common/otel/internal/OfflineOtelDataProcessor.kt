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

package com.splunk.rum.common.otel.internal

import android.content.Context
import com.splunk.android.common.job.IJobManager
import com.splunk.android.common.job.JobIdStorage
import com.splunk.android.common.job.JobManager
import com.splunk.android.common.logger.Logger
import com.splunk.android.common.utils.extensions.safeSubmit
import com.splunk.rum.common.otel.logRecord.UploadOtelLogRecordData
import com.splunk.rum.common.otel.span.UploadOtelSpanData
import com.splunk.rum.common.storage.AgentStorage
import com.splunk.rum.common.storage.IAgentStorage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class OfflineOtelDataProcessor internal constructor(
    private val agentStorage: IAgentStorage,
    private val jobIdStorage: JobIdStorage,
    private val jobManager: IJobManager
) {

    private val executor = Executors.newCachedThreadPool()
    private var loadedLocalData = AtomicBoolean(false)

    /**
     * Kicks off background uploads for any locally stored telemetry data older than [olderThan].
     * [olderThan] is needed to not double upload data which is generated in current run.
     * Subsequent calls after the first invocation are ignored to avoid scheduling duplicate jobs.
     */
    fun start(olderThan: Long) {
        executor.safeSubmit {
            Logger.d(TAG, "start(): called")

            if (!loadedLocalData.getAndSet(true)) {
                startProcessingLocalData(olderThan)
            } else {
                Logger.d(TAG, "start(): already called! Not doing anything.")
            }
        }
    }

    private fun startProcessingLocalData(olderThan: Long) {
        agentStorage.getLogs(olderThan).map { it.name }.forEach {
            cancelJob(it)
            jobManager.scheduleJob(UploadOtelLogRecordData(it, jobIdStorage))
        }

        agentStorage.getSpans(olderThan).map { it.name }.forEach {
            cancelJob(it)
            jobManager.scheduleJob(UploadOtelSpanData(it, jobIdStorage))
        }

        agentStorage.getSessionReplayData(olderThan).map { it.name }.forEach {
            cancelJob(it)
            jobManager.scheduleJob(UploadOtelSpanData(it, jobIdStorage))
        }
    }

    private fun cancelJob(id: String) {
        val jobId = jobIdStorage.get(id) ?: return
        jobManager.cancel(jobId)
        jobIdStorage.delete(id)
    }

    companion object {
        private const val TAG = "OfflineOtelDataProcessor"

        private val lock = Any()

        private var instance: OfflineOtelDataProcessor? = null

        fun attach(context: Context): OfflineOtelDataProcessor = synchronized(lock) {
            instance ?: run {
                val agentStorage = AgentStorage.attach(context)
                val jobManager = JobManager.attach(context)
                val jobIdStorage = JobIdStorage.init(context, isEncrypted = false)

                OfflineOtelDataProcessor(
                    agentStorage = agentStorage,
                    jobManager = jobManager,
                    jobIdStorage = jobIdStorage
                ).also {
                    instance = it
                    Logger.v(TAG, "attach(): OfflineOtelDataProcessor attached.")
                }
            }
        }
    }
}
