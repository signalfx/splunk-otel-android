/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.sdk.common.storage.policy

import java.io.File
import kotlin.math.min

/**
 * [StoragePolicy] is used to define restrictions for a specific section (region) of stored data.
 */
internal data class StoragePolicy(
    val dir: File,
    val maxOccupiedSpace: Long,
    val maxOccupiedSpacePercentage: Float,
    val minStorageSpaceLeft: Long
) {
    fun check(freeSpace: Long): Boolean {
        val size = SizeCache.dirSize(dir)
        val maximalSize = min(maxOccupiedSpace, (maxOccupiedSpacePercentage * freeSpace).toLong())
        return size < maximalSize && freeSpace > minStorageSpaceLeft
    }
}
