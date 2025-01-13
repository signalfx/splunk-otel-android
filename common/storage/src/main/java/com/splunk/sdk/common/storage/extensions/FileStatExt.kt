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

package com.splunk.sdk.common.storage.extensions

import android.os.Build
import android.os.StatFs
import com.cisco.android.common.utils.runOnAndroidAtLeast

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
