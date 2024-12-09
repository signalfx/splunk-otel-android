package com.smartlook.sdk.common.job

import com.smartlook.sdk.common.utils.extensions.toJSONObject
import com.smartlook.sdk.common.storage.IStorage
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class JobIdStorage(
    private val storage: IStorage
) {
    /**
     * Because the internal policy of [JobSchedulerWorker] does not allow IDs as [String] we
     * have to map each [String] to unique [Int].
     */
    private var stringIntIdMap: StringIntIdMap
        set(value) {
            storage.writeJobIdTable(value.toJSONObject().toString())
        }
        get() {
            val rawString = storage.readJobIdTable() ?: return StringIntIdMap()
            return StringIntIdMap.fromJSONObject(rawString.toJSONObject())
        }

    /**
     * In order to prevent problems when creating new id. We keep track of the latest id
     * and increment it by one every time new id is requested.
     */
    private var lastId: Int
        set(value) {
            storage.writeJobIdTableLastNumber(value)
        }
        get() = storage.readJobIdTableLastNumber() ?: 0

    private val readWriteLock = ReentrantLock()

    fun getOrCreateId(stringId: String): Int {
        return readWriteLock.withLock {
            val map = stringIntIdMap
            val latestId = lastId
            val id = map[stringId] ?: run {
                val newId = if (latestId >= ID_LIMIT) {
                    0
                } else {
                    latestId + 1
                }
                this.lastId = newId
                newId
            }

            // This should never happen. But as a safety mechanism it is here.
            if (map.size > SAFETY_SIZE) {
                map.clear()
            }

            map[stringId] = id

            this.stringIntIdMap = map
            id
        }
    }

    fun get(id: String): Int? {
        return readWriteLock.withLock {
            stringIntIdMap[id]
        }
    }

    fun getAll(): Map<String, Int> = readWriteLock.withLock { stringIntIdMap }

    fun getAllWithPrefix(prefix: String): Map<String, Int> {
        return readWriteLock.withLock {
            val map = stringIntIdMap
            map.filter { it.key.startsWith(prefix) }
        }
    }

    fun deleteAllWithPrefix(prefix: String) {
        readWriteLock.withLock {
            val map = stringIntIdMap
            map.keys.filter { it.startsWith(prefix) }.forEach { map.remove(it) }
            stringIntIdMap = map
        }
    }

    fun delete(key: String) {
        readWriteLock.withLock {
            val map = stringIntIdMap
            map.remove(key)
            stringIntIdMap = map
        }
    }

    /**
     * Json serializable/deserializable wrapper for [HashMap].
     */
    internal class StringIntIdMap : HashMap<String, Int>() {
        fun toJSONObject(): JSONObject {
            return JSONObject().apply {
                forEach {
                    put(it.key, it.value)
                }
            }
        }

        companion object {
            fun fromJSONObject(jsonObject: JSONObject) = StringIntIdMap().apply {
                jsonObject.keys().forEach {
                    put(it, jsonObject[it] as Int)
                }
            }
        }
    }

    companion object {
        private const val ID_LIMIT = Int.MAX_VALUE - 10000
        private const val SAFETY_SIZE = 10000
        private var instance: JobIdStorage? = null

        fun init(storage: IStorage): JobIdStorage {
            return instance ?: JobIdStorage(storage).also { instance = it }
        }
    }
}
