package com.splunk.sdk.common.storage.preferences

interface IPermanentCache {
    fun load(): String
    fun save(jsonString: String)
}
