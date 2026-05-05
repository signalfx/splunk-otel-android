/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.common.storage

/**
 * Represents the persisted endpoint configuration as a single atomic unit.
 * Stored as a single JSON string in preferences to prevent partial-write
 * inconsistencies between URL and token.
 *
 * Uses manual JSON serialization to avoid dependency on android.json
 * which is unavailable in JVM unit tests.
 */
data class StoredEndpointConfig(
    val tracesBaseUrl: String,
    val logsBaseUrl: String?,
    val rumAccessToken: String?
) {
    fun toJson(): String {
        val sb = StringBuilder()
        sb.append('{')
        sb.append(jsonString(KEY_TRACES_URL)).append(':').append(jsonString(tracesBaseUrl))
        sb.append(',')
        sb.append(jsonString(KEY_LOGS_URL)).append(':').append(jsonStringOrNull(logsBaseUrl))
        sb.append(',')
        sb.append(jsonString(KEY_TOKEN)).append(':').append(jsonStringOrNull(rumAccessToken))
        sb.append('}')
        return sb.toString()
    }

    companion object {
        private const val KEY_TRACES_URL = "tracesBaseUrl"
        private const val KEY_LOGS_URL = "logsBaseUrl"
        private const val KEY_TOKEN = "rumAccessToken"

        fun fromJson(json: String): StoredEndpointConfig? {
            return try {
                val map = parseJsonObject(json) ?: return null
                val tracesUrl = map[KEY_TRACES_URL] ?: return null
                StoredEndpointConfig(
                    tracesBaseUrl = tracesUrl,
                    logsBaseUrl = map[KEY_LOGS_URL],
                    rumAccessToken = map[KEY_TOKEN]
                )
            } catch (_: Exception) {
                null
            }
        }

        private fun jsonString(value: String): String {
            val escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            return "\"$escaped\""
        }

        private fun jsonStringOrNull(value: String?): String =
            if (value == null) "null" else jsonString(value)

        /**
         * Minimal JSON object parser that handles string values and null.
         * Returns a map of key to nullable string value.
         */
        private fun parseJsonObject(json: String): Map<String, String?>? {
            val trimmed = json.trim()
            if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) return null
            val inner = trimmed.substring(1, trimmed.length - 1)
            val result = mutableMapOf<String, String?>()
            var pos = 0

            while (pos < inner.length) {
                pos = skipWhitespace(inner, pos)
                if (pos >= inner.length) break

                val (key, nextAfterKey) = readString(inner, pos) ?: return null
                pos = skipWhitespace(inner, nextAfterKey)
                if (pos >= inner.length || inner[pos] != ':') return null
                pos++
                pos = skipWhitespace(inner, pos)

                if (inner.startsWith("null", pos)) {
                    result[key] = null
                    pos += 4
                } else {
                    val (value, nextAfterVal) = readString(inner, pos) ?: return null
                    result[key] = value
                    pos = nextAfterVal
                }

                pos = skipWhitespace(inner, pos)
                if (pos < inner.length && inner[pos] == ',') pos++
            }
            return result
        }

        private fun skipWhitespace(s: String, start: Int): Int {
            var i = start
            while (i < s.length && s[i].isWhitespace()) i++
            return i
        }

        /**
         * Reads a JSON-quoted string starting at [start].
         * Returns the unescaped string value and the index after the closing quote.
         */
        private fun readString(s: String, start: Int): Pair<String, Int>? {
            if (start >= s.length || s[start] != '"') return null
            val sb = StringBuilder()
            var i = start + 1
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    when (s[i + 1]) {
                        '"' -> { sb.append('"'); i += 2 }
                        '\\' -> { sb.append('\\'); i += 2 }
                        'n' -> { sb.append('\n'); i += 2 }
                        'r' -> { sb.append('\r'); i += 2 }
                        't' -> { sb.append('\t'); i += 2 }
                        else -> { sb.append(s[i + 1]); i += 2 }
                    }
                } else if (c == '"') {
                    return sb.toString() to (i + 1)
                } else {
                    sb.append(c)
                    i++
                }
            }
            return null
        }
    }
}
