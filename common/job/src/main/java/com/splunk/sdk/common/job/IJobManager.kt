package com.splunk.sdk.common.job

interface IJobManager {
    fun scheduleJob(jobType: JobType)

    fun isJobScheduled(id: Int): Boolean

    fun cancelAll()

    fun cancel(id: Int)
}
