package com.splunk.sdk.common.job

import android.app.job.JobInfo
import android.content.Context

interface JobType {
    val jobNumberLimit: Long?

    fun createJobInfo(context: Context): JobInfo

    /**
     * @return True is [jobNumberLimit] is null or it is less then number of currently scheduled
     * jobs.
     */
    fun canSchedule(currentSize: Int): Boolean {
        val limit = jobNumberLimit
        return limit == null || currentSize <= limit
    }
}
