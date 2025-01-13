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

import android.app.job.JobInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.cisco.android.common.job.JobIdStorage
import com.cisco.android.common.job.JobType

internal data class UploadOtelSpanData(val id: String, val jobIdStorage: JobIdStorage) : JobType {

    override val jobNumberLimit: Long = 80L

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun createJobInfo(context: Context): JobInfo {
        return UploadOtelSpanDataJob.createJobInfoBuilder(context, jobIdStorage.getOrCreateId(id), id).build()
    }
}
