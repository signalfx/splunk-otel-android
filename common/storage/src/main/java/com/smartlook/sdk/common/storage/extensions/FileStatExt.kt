package com.smartlook.sdk.common.storage.extensions

import android.os.Build
import android.os.StatFs
import com.smartlook.sdk.common.utils.runOnAndroidAtLeast

internal val StatFs.blockSizeCompat: Long
    get() = runOnAndroidAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR2) { blockSizeLong } ?: blockSizeDeprecated

internal val StatFs.availableBlocksCompat: Long
    get() = runOnAndroidAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR2) { availableBlocksLong } ?: availableBlocksDeprecated

// Suppressed deprecated variants

private val StatFs.blockSizeDeprecated: Long
    @Suppress("DEPRECATION")
    get() = blockSize.toLong()

private val StatFs.availableBlocksDeprecated: Long
    @Suppress("DEPRECATION")
    get() = availableBlocks.toLong()
