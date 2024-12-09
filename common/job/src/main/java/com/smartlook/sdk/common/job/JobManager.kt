package com.smartlook.sdk.common.job

import android.annotation.SuppressLint
import android.app.job.JobScheduler
import android.content.Context
import android.os.Build
import com.smartlook.sdk.log.LogAspect
import com.smartlook.sdk.common.logger.Logger

/**
 * Takes care of scheduling the rests to send them only if they meet certain conditions.
 */
@SuppressLint("NewApi")
class JobManager(private val context: Context) : IJobManager {

    private val jobScheduler: JobScheduler by lazy { context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler }

    override fun scheduleJob(jobType: JobType) {
        Logger.privateD(LogAspect.JOB, TAG, { "scheduleJob()" })
        val jobInfo = jobType.createJobInfo(context = context)
        try {
            if (jobType.canSchedule(jobScheduler.allPendingJobs.size)) {
                val result = jobScheduler.schedule(jobInfo)
                if (result == JobScheduler.RESULT_FAILURE) {
                    Logger.privateD(LogAspect.JOB, TAG, { "scheduleJob(): job was not scheduled, failure" })
                }
            } else {
                Logger.privateD(LogAspect.JOB, TAG, { "scheduleJob(): job was not scheduled, limit was reached" })
            }
        } catch (exception: Exception) {
            Logger.privateD(LogAspect.JOB, TAG, { "scheduleJob(): job was not scheduled, limit was reached" })
        }
    }

    override fun isJobScheduled(id: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            jobScheduler.getPendingJob(id) != null
        } else {
            jobScheduler.allPendingJobs.any { jobInfo -> jobInfo.id == id }
        }
    }

    override fun cancelAll() {
        jobScheduler.cancelAll()
    }

    override fun cancel(id: Int) {
        // System is throwing unexpected exception on some specific devices -> JobSchedulerImpl line 74.
        try {
            jobScheduler.cancel(id)
        } catch (exception: Exception) {
        }
    }

    companion object {
        private const val TAG = "JobManager"

        private var instance: IJobManager? = null

        fun attach(context: Context): IJobManager {
            Logger.privateV(LogAspect.JOB, TAG, { "attach(): JobManager attached." })
            return instance ?: JobManager(context).also { instance = it }
        }
    }
}
