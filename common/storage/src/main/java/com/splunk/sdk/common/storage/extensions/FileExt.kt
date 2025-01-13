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

import android.os.StatFs
import java.io.File

internal fun File.createNewFileOnPath(): File {
    if (!exists()) {
        this.parentFile?.mkdirs()
        this.createNewFile()
    }
    return this
}

internal val File.statFsFreeSpace: Long
    get() {
        if (!exists())
            mkdirs()

        return StatFs(path).run { availableBlocksCompat * blockSizeCompat }
    }

internal fun File.formatChildFile(vararg pathParts: String): File {
    return File(this, pathParts.joinToString(separator = File.separator))
}

internal fun File.formatChildDir(vararg pathParts: String): File {
    return File(this, pathParts.joinToString(separator = File.separator, postfix = File.separator))
}

internal fun File.oldestChildDir(): File? = listFiles()?.filter { isDirectory }?.minByOrNull { it.lastModified() }
