package com.smartlook.sdk.common.storage.preferences

interface IPreferences {
    fun putString(key: String, value: String): IPreferences

    fun putInt(key: String, value: Int): IPreferences

    fun putLong(key: String, value: Long): IPreferences

    fun putFloat(key: String, value: Float): IPreferences

    fun putBoolean(key: String, value: Boolean): IPreferences

    fun putStringMap(key: String, value: Map<String, String>): IPreferences

    fun apply()

    fun commit()

    fun remove(key: String): IPreferences

    fun clear(): IPreferences

    fun getString(key: String): String?

    fun getInt(key: String): Int?

    fun getLong(key: String): Long?

    fun getFloat(key: String): Float?

    fun getBoolean(key: String): Boolean?

    fun getStringMap(key: String): Map<String, String>?

    operator fun contains(key: String): Boolean

    fun size(): Int
}
