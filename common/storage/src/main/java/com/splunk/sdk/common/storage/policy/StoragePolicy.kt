package com.splunk.sdk.common.storage.policy

import java.io.File

/**
 * [StoragePolicy] is used to define restrictions for a specific section (region) of stored data.
 */
internal data class StoragePolicy(
    val dir: File,
    val maxOccupiedSpace: Long,
    val maxOccupiedSpacePercentage: Float,
    val minStorageSpaceLeft: Long
)
