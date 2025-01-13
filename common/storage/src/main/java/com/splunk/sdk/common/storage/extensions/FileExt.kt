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
