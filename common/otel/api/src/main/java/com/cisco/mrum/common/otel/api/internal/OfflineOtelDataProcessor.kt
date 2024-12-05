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

package com.cisco.mrum.common.otel.api.internal

import com.smartlook.sdk.common.job.IJobManager
import com.smartlook.sdk.common.job.JobIdStorage
import com.smartlook.sdk.common.logger.Logger
import com.smartlook.sdk.common.storage.IStorage
import com.smartlook.sdk.common.utils.extensions.safeSubmit
import com.smartlook.sdk.log.LogAspect
import com.cisco.mrum.common.otel.api.logRecord.UploadOtelLogRecordData
import com.cisco.mrum.common.otel.api.span.UploadOtelSpanData
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal class OfflineOtelDataProcessor(
    private val storage: IStorage,
    private val jobIdStorage: JobIdStorage,
    private val jobManager: IJobManager
) {

    private val executor = Executors.newCachedThreadPool()
    private var loadedLocalData = AtomicBoolean(false)

    fun start() {
        executor.safeSubmit {
            Logger.d(LogAspect.SDK_METHODS, TAG) { "start(): called" }

            if (!loadedLocalData.getAndSet(true)) {
                startProcessingLocalData()
            } else {
                Logger.d(LogAspect.SDK_METHODS, TAG) { "start(): already called! Not doing anything." }
            }
        }
    }

    private fun startProcessingLocalData() {
        storage.getOtelLogDataDir().listFiles()?.map { it.name }?.forEach {
            cancelJob(it)
            jobManager.scheduleJob(UploadOtelLogRecordData(it, jobIdStorage))
        }

        storage.getOtelSpanDataDir().listFiles()?.map { it.name }?.forEach {
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
    }
}
