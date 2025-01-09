package com.splunk.sdk.common.storage.preferences

import com.cisco.android.common.logger.Logger
import java.io.File

class FilePermanentCache(private val file: File) : IPermanentCache {

    override fun load(): String {
        if (!file.exists()) {
            return "{}"
        }

        var jsonString = ""
        try {
            jsonString = file.readText()
        } catch (e: Exception) {
            Logger.w(Preferences.TAG, "loadFromFile(): Failed to load from a file due to ${e.message}!")
        }

        return jsonString
    }

    override fun save(jsonString: String) {
        try {
            file.writeText(jsonString)
        } catch (e: Exception) {
            Logger.w(Preferences.TAG, "commit(): Failed to write text due to ${e.message}!")
        }
    }
}
