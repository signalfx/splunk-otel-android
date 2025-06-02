package com.splunk.sdk.common.otel.span

import android.app.job.JobInfo
import android.content.Context
import com.cisco.android.common.job.JobIdStorage
import com.cisco.android.common.job.JobType

internal data class UploadSessionReplayData(val id: String, val jobIdStorage: JobIdStorage) : JobType {

    override val jobNumberLimit: Long = 80L

    override fun createJobInfo(context: Context): JobInfo =
        UploadSessionReplayDataJob.createJobInfoBuilder(context, jobIdStorage.getOrCreateId(id), id).build()
}
