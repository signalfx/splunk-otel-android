package com.smartlook.sdk.common.logger

import android.os.Build
import android.util.Log
import kotlin.math.min

class LogPrinter(
    private val defaultTag: String = TAG,
    private val tagPrefix: String = TAG_PREFIX
) {

    fun print(severity: Int, aspect: Long, tag: String?, message: String) {
        val processedTag = processTag(tag)

        if (message.length < MAX_LOG_LENGTH) {
            printLine(severity, processedTag, message)
            return
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != NEW_LINE_NOT_FOUND) newline else length
            do {
                val end = min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                printLine(severity, processedTag, part)
                i = end
            } while (i < newline)
            i++
        }
    }

    private fun printLine(severity: Int, tag: String, message: String) {
        if (severity == Log.ASSERT) {
            Log.e(tag, message)
        } else {
            Log.println(severity, tag, message)
        }
    }

    private fun processTag(tag: String?): String {
        return if (tag == null) {
            defaultTag
        } else {
            val prefixedTag = "$tagPrefix$tag"
            if (prefixedTag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                prefixedTag
            } else {
                prefixedTag.substring(0, MAX_TAG_LENGTH)
            }
        }
    }

    companion object {
        private const val TAG = "Smartlook"
        private const val TAG_PREFIX = "SL_"

        private const val NEW_LINE_NOT_FOUND = -1
        private const val MAX_LOG_LENGTH = 4000
        private const val MAX_TAG_LENGTH = 23
    }
}
