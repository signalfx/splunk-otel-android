package com.smartlook.sdk.common.storage.preferences

import com.smartlook.sdk.common.utils.Lock
import com.smartlook.sdk.log.LogAspect
import com.smartlook.sdk.common.logger.Logger
import com.smartlook.sdk.common.utils.extensions.safeSubmit
import org.json.JSONException
import java.util.concurrent.Executors
import kotlin.collections.set

// TODO cant be internal due to testing. Inside of smartlooksdk module. Suggestions are welcome
class Preferences(private val permanentCache: IPermanentCache) : IPreferences {
    private val map = hashMapOf<String, Value>()
    private val lockLoad = Lock()
    private val lockSave = Lock()
    private val executor = Executors.newSingleThreadExecutor()

    init {
        loadFromPermanentCache()
    }

    override fun putString(key: String, value: String): IPreferences {
        return putValue(key, StringValue(value))
    }

    override fun putInt(key: String, value: Int): IPreferences {
        return putValue(key, IntValue(value))
    }

    override fun putLong(key: String, value: Long): IPreferences {
        return putValue(key, LongValue(value))
    }

    override fun putFloat(key: String, value: Float): IPreferences {
        return putValue(key, FloatValue(value))
    }

    override fun putBoolean(key: String, value: Boolean): IPreferences {
        return putValue(key, BooleanValue(value))
    }

    override fun putStringMap(key: String, value: Map<String, String>): IPreferences {
        return putValue(key, StringMapValue(value))
    }

    override fun apply() {
        lockLoad.waitToUnlock()

        val jsonString = synchronized(map) {
            serializeFromValueMap(map)
        }

        lockSave.lock()
        executor.safeSubmit {
            permanentCache.save(jsonString)
            lockSave.unlock()
        }
    }

    override fun commit() {
        lockLoad.waitToUnlock()

        val jsonString = synchronized(map) {
            serializeFromValueMap(map)
        }

        lockSave.waitToUnlock()
        permanentCache.save(jsonString)
    }

    override fun remove(key: String): IPreferences {
        lockLoad.waitToUnlock()

        synchronized(map) {
            map -= key
        }
        return this
    }

    override fun clear(): IPreferences {
        lockLoad.waitToUnlock()

        synchronized(map) {
            map.clear()
        }
        return this
    }

    override fun getString(key: String): String? = getValue(key)

    override fun getInt(key: String): Int? = getValue(key)

    override fun getLong(key: String): Long? = getValue(key)

    override fun getFloat(key: String): Float? = getValue(key)

    override fun getBoolean(key: String): Boolean? = getValue(key)

    override fun getStringMap(key: String): Map<String, String>? = getValue(key)

    override operator fun contains(key: String): Boolean {
        lockLoad.waitToUnlock()

        return synchronized(map) { map.contains(key) }
    }

    override fun size(): Int {
        lockLoad.waitToUnlock()
        return synchronized(map) { map.size }
    }

    private fun loadFromPermanentCache() {
        lockLoad.lock()

        executor.safeSubmit {
            val jsonString = permanentCache.load()

            if (jsonString.isEmpty()) {
                lockLoad.unlock()
                return@safeSubmit
            }

            synchronized(map) {
                try {
                    deserializeAndFillMap(jsonString, map)
                } catch (e: JSONException) {
                    // If cache gets corrupted because of sudden crash it needs to be cleared
                    commit()
                    Logger.w(LogAspect.STORAGE, TAG) {
                        "deserializeAndFillMap(): Failed to deserialize a String due to ${e.message}!"
                    }

                    lockLoad.unlock()
                    return@safeSubmit
                }
            }

            lockLoad.unlock()
        }
    }

    private fun putValue(key: String, value: Value): IPreferences {
        lockLoad.waitToUnlock()

        synchronized(map) {
            map[key] = value
        }

        return this
    }

    private inline fun <reified T> getValue(key: String): T? {
        lockLoad.waitToUnlock()

        val wrappedValue = synchronized(map) { map[key] } ?: return null

        @Suppress("IMPLICIT_CAST_TO_ANY")
        val value = when (wrappedValue) {
            is StringValue -> wrappedValue.value
            is IntValue -> wrappedValue.value
            is LongValue -> wrappedValue.value
            is FloatValue -> wrappedValue.value
            is BooleanValue -> wrappedValue.value
            is StringMapValue -> wrappedValue.value
        }

        return value as? T ?: throw IllegalArgumentException("Expected a value of type ${T::class}, but got ${value::class}!")
    }

    companion object {
        internal const val TAG = "Preferences"
    }
}
