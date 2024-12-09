package com.smartlook.sdk.common.storage

import android.content.Context
import android.graphics.Bitmap
import com.smartlook.sdk.common.utils.runOnBackgroundThread
import com.smartlook.sdk.log.LogAspect
import com.smartlook.sdk.common.logger.Logger
import com.smartlook.sdk.common.storage.extensions.MB
import com.smartlook.sdk.common.storage.extensions.createNewFileOnPath
import com.smartlook.sdk.common.storage.extensions.oldestChildDir
import com.smartlook.sdk.common.storage.extensions.statFsFreeSpace
import com.smartlook.sdk.common.storage.policy.SizeCache
import com.smartlook.sdk.common.storage.policy.StoragePolicy
import com.smartlook.sdk.common.storage.preferences.FilePermanentCache
import com.smartlook.sdk.common.storage.preferences.Preferences
import java.io.File
import java.lang.Long.min

/**
 * Ideas:
 * - Remove the unnecessary folders with single files in them.
 * - Video image storage needs rework!
 * - If we rework the network layer we can make paths private, because they are now used only for the FILE multipart parts.
 */
class Storage private constructor(context: Context) : IStorage {
    override val freeSpace: Long
        get() {
            val freeSpace = paths.systemRootDir.statFsFreeSpace
            Logger.privateV(LogAspect.STORAGE, TAG, { "freeSpace: $freeSpace" })
            return freeSpace
        }

    override val consistentDirPath: String
        get() {
            val path = paths.rootDir.path
            Logger.privateV(LogAspect.STORAGE, TAG, { "consistentDirPath: $path" })
            return path
        }

    override val isSessionStorageFull: Boolean
        get() {
            val isFull = !checkPolicy(StoragePolicy(paths.sessionsDir, 1000.MB, 0.2f, 50.MB))
            Logger.privateV(LogAspect.STORAGE, TAG, { "isSessionStorageFull: $isFull" })
            return isFull
        }

    private val paths: Paths = Paths(context)

    override val preferences = Preferences(FilePermanentCache(paths.preferencesFile))

    //region Record

    override fun writeRecord(sessionId: String, recordIndex: Int, recordJson: String): Boolean {
        val recordFile: File = paths.recordFile(sessionId, recordIndex)
        val success = runCatching {
            recordFile.createNewFileOnPath()
            recordFile.writeText(recordJson)
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "writeRecord(): sessionId = $sessionId, recordIndex = $recordIndex, success = $success"
            }
        )

        return success
    }

    override fun readRecord(sessionId: String, recordIndex: Int): String? {
        val recordFile: File = paths.recordFile(sessionId, recordIndex)
        val record = runCatching { recordFile.readText() }.getOrNull()

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "readRecord(): sessionId = $sessionId, recordIndex = $recordIndex, isNullOrBlank = ${record.isNullOrBlank()}"
            }
        )

        return record
    }

    override fun deleteRecord(sessionId: String, recordIndex: Int): Boolean {
        val recordDir: File = paths.recordDir(sessionId, recordIndex)
        val success = runCatching {
            recordDir.deleteRecursively()
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "deleteRecord(): sessionId = $sessionId, recordIndex = $recordIndex, success = $success"
            }
        )

        return success
    }

    //endregion

    //region Sessions

    override fun getSessionIds(): List<String> {
        val sessionIds = runCatching {
            paths.sessionsDir.listFiles()?.map { it.name } ?: emptyList()
        }.getOrElse { emptyList() }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "getSessionIds(): sessionIds = [${sessionIds.joinToString()}]"
            }
        )

        return sessionIds
    }

    //endregion

    //region Session

    override fun hasSessionData(sessionId: String): Boolean {
        val sessionDir: File = paths.sessionDir(sessionId)
        val recordsDir: File = paths.recordsDir(sessionId)

        val hasData = runCatching {
            val isSessionDirEmpty = sessionDir.listFiles()?.isEmpty() ?: true
            val isRecordDirEmpty = recordsDir.listFiles()?.isEmpty() ?: true
            !isSessionDirEmpty && !isRecordDirEmpty
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "hasSessionData(): sessionId = $sessionId, hasData = $hasData"
            }
        )

        return hasData
    }

    override fun getRecordIndexes(sessionId: String): List<Int> {
        val recordsDir = paths.recordsDir(sessionId)
        val recordIndexes = runCatching {
            recordsDir.listFiles()?.map { it.name.toInt() } ?: emptyList()
        }.getOrElse { emptyList() }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "getRecordIndexes(): recordIndexes = [${recordIndexes.joinToString()}]"
            }
        )

        return recordIndexes
    }

    override fun deleteSession(sessionId: String): Boolean {
        val sessionDir: File = paths.sessionDir(sessionId)
        val success = runCatching {
            sessionDir.deleteRecursively()
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "deleteSession(): sessionId = $sessionId, success = $success"
            }
        )

        return success
    }

    override fun deleteOldestSession(excludeSessionId: String): String? {
        val sessionIdToDelete = paths.sessionsDir.oldestChildDir()?.name
        var deletedSessionId: String? = null
        var successfulDelete = false

        if (sessionIdToDelete != null && sessionIdToDelete != excludeSessionId) {
            if (deleteSession(sessionIdToDelete)) {
                deletedSessionId = sessionIdToDelete
                successfulDelete = true
            }
        }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "deleteOldestSession(): deletedSessionId = $deletedSessionId, successfulDelete = $successfulDelete"
            }
        )

        return deletedSessionId
    }

    //endregion

    //region Wireframe

    override fun getWireframeFile(sessionName: String, recordIndex: Int): File {
        return paths.wireframeFile(sessionName, recordIndex)
    }

    override fun isWireframeFileAvailable(sessionId: String, recordIndex: Int): Boolean {
        val wireframeFile: File = paths.wireframeFile(sessionId, recordIndex)
        val isAvailable = runCatching { wireframeFile.exists() }.getOrElse { false }

        Logger.privateD(LogAspect.STORAGE, TAG, { "isWireframeFileAvailable(): isAvailable = $isAvailable" })

        return isAvailable
    }

    override fun writeWireframe(sessionId: String, recordIndex: Int, wireframe: ByteArray): Boolean {
        val wireframeFile: File = paths.wireframeFile(sessionId, recordIndex)
        val success = runCatching {
            wireframeFile.createNewFileOnPath()
            wireframeFile.writeBytes(wireframe)
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE,
            TAG,
            { "writeWireframe(): sessionId = $sessionId, recordIndex = $recordIndex, success = $success" })

        return success
    }

    //endregion

    //region Metrics

    override fun writeMetrics(sessionId: String, recordIndex: Int, metrics: String): Boolean {
        val metricsFile: File = paths.metricsFile(sessionId, recordIndex)
        val success = runCatching {
            metricsFile.createNewFileOnPath()
            metricsFile.writeText(metrics)
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE,
            TAG,
            { "writeMetrics(): sessionId = $sessionId, recordIndex = $recordIndex, success = $success" })

        return success
    }

    override fun readMetrics(sessionId: String, recordIndex: Int): String? {
        val metricsFile: File = paths.metricsFile(sessionId, recordIndex)
        val metrics = runCatching { metricsFile.readText() }.getOrNull()

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "readMetrics(): sessionId = $sessionId, recordIndex = $recordIndex, isNullOrBlank = ${metrics.isNullOrBlank()}"
            }
        )

        return metrics
    }

    //endregion

    //region Session video images

    override fun getVideoImageDir(sessionId: String, recordIndex: Int): File {
        return paths.videoImageDir(sessionId, recordIndex)
    }

    //endregion

    //region Video configuration

    override fun writeVideoConfig(sessionId: String, recordIndex: Int, videoConfiguration: String): Boolean {
        val videoConfigurationFile: File = paths.videoConfigFile(sessionId, recordIndex)
        val success = runCatching {
            videoConfigurationFile.createNewFileOnPath()
            videoConfigurationFile.writeText(videoConfiguration)
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "writeVideoConfig(): sessionId = $sessionId, recordIndex = $recordIndex, success = $success"
            }
        )

        return success
    }

    override fun readVideoConfig(sessionId: String, recordIndex: Int): String? {
        val videoConfigFile = paths.videoConfigFile(sessionId, recordIndex)
        val videoConfig = runCatching { videoConfigFile.readText() }.getOrNull()

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "readVideoConfig(): sessionId = $sessionId, recordIndex = $recordIndex, isNullOrBlank = ${videoConfig.isNullOrBlank()}"
            }
        )

        return videoConfig
    }

    //endregion

    //region Video frames

    override fun writeVideoFrame(
        sessionId: String,
        recordIndex: Int,
        frameIndex: Int,
        frame: Bitmap,
        format: Bitmap.CompressFormat,
        quality: Int
    ): Boolean {
        val videoImageFile = paths.videoImageFile(sessionId, recordIndex, frameIndex)
        val success = runCatching {
            videoImageFile.createNewFileOnPath()
            videoImageFile.outputStream().use { out ->
                frame.compress(format, quality, out)
                out.flush()
            }
            true
        }.getOrElse { false }

        Logger.privateV(
            LogAspect.STORAGE, TAG,
            {
                "writeVideoFrame(): sessionId = $sessionId, recordIndex = $recordIndex," +
                    " frameIndex = $frameIndex, success = $success, width: ${frame.width}, height: ${frame.height}"
            }
        )

        return success
    }

    override fun deleteAllVideoFrames(sessionId: String, recordIndex: Int): Boolean {
        val videoImageDir = paths.videoImageDir(sessionId, recordIndex)
        val videoImageFiles = videoImageDir.listFiles { _, name -> name.endsWith(".jpg") }
        var success = true

        videoImageFiles?.forEach {
            runCatching {
                it.delete()
            }.onFailure {
                success = false
            }
        }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "deleteAllVideoFrames(): sessionId = $sessionId, recordIndex = $recordIndex, success = $success"
            }
        )

        return success
    }

    //endregion

    //region Video file

    override fun createVideoFile(sessionId: String, recordIndex: Int): File {
        return getVideoFile(sessionId, recordIndex).createNewFileOnPath()
    }

    override fun getVideoFile(sessionId: String, recordIndex: Int): File {
        return paths.videoFile(sessionId, recordIndex)
    }

    override fun isVideoFileAvailable(sessionId: String, recordIndex: Int): Boolean {
        val videoFile = paths.videoFile(sessionId, recordIndex)
        val isAvailable = runCatching { videoFile.exists() }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "isVideoFileAvailable(): sessionId = $sessionId, recordIndex = $recordIndex, isAvailable = $isAvailable"
            }
        )

        return isAvailable
    }

    //endregion

    //region Identification

    override fun writeIdentification(visitorId: String, identification: String): Boolean {
        val identificationFile = paths.identificationFile(visitorId)
        val success = runCatching {
            identificationFile.createNewFileOnPath()
            identificationFile.writeText(identification)
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "writeIdentification(): visitorId = $visitorId, success = $success"
            }
        )

        return success
    }

    override fun readIdentification(visitorId: String): String? {
        val identificationFile = paths.identificationFile(visitorId)
        val identification = runCatching { identificationFile.readText() }.getOrElse { null }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "readIdentification(): visitorId = $visitorId, isNullOrBlank = ${identification.isNullOrBlank()}"
            }
        )

        return identification
    }

    override fun deleteIdentification(visitorId: String): Boolean {
        val identificationDir = paths.identificationDir(visitorId)
        val success = runCatching {
            identificationDir.deleteRecursively()
            true
        }.getOrElse { false }

        Logger.privateD(
            LogAspect.STORAGE, TAG,
            {
                "deleteIdentification(): visitorId = $visitorId, success = $success"
            }
        )

        return success
    }

    //endregion

    //region Consistency

    override fun deleteInconsistentPath(path: String) {
        val dir = File(path)
        runOnBackgroundThread {
            val success = runCatching {
                dir.deleteRecursively()
                true
            }.getOrElse { false }

            Logger.w(LogAspect.STORAGE, TAG) { "deleteInconsistentDir(): path = $path, success = $success" }
        }
    }

    override fun invalidateAllPreferences() {
        preferences.clear().apply()
    }

    //endregion

    //region Otel

    override fun getOtelLogDataDir(): File = paths.otelLogDir
    override fun getOtelSpanDataDir(): File = paths.otelSpanDir

    override fun createOtelLogDataFile(id: String): File = paths.otelLogFile(id).createNewFileOnPath()
    override fun getOtelLogDataFile(id: String): File = paths.otelLogFile(id)

    override fun createOtelSpanDataFile(id: String): File = paths.otelSpanFile(id).createNewFileOnPath()
    override fun getOtelSpanDataFile(id: String): File = paths.otelSpanFile(id)

    //endregion

    //region Storage space

    private fun checkPolicy(policy: StoragePolicy): Boolean {
        val size = SizeCache.dirSize(policy.dir)
        val maximalSize = min(
            policy.maxOccupiedSpace,
            (policy.maxOccupiedSpacePercentage * freeSpace).toLong()
        )
        return size < maximalSize && freeSpace > policy.minStorageSpaceLeft
    }

    //endregion

    //region Preferences

    override fun writeLastStorageDirPath(value: String) {
        preferences.putString(LAST_STORAGE_DIR_PATH, value).apply()
    }

    override fun deleteLastStorageDirPath() {
        preferences.remove(LAST_STORAGE_DIR_PATH).apply()
    }

    override fun readLastStorageDirPath(): String? = preferences.getString(LAST_STORAGE_DIR_PATH)

    override fun writeLastVisitorId(value: String) {
        preferences.putString(LAST_VISITOR_ID, value).apply()
    }

    override fun deleteLastVisitorId() {
        preferences.remove(LAST_VISITOR_ID).apply()
    }

    override fun readLastVisitorId(): String? = preferences.getString(LAST_VISITOR_ID)

    override fun writeSessionToVisitorMap(value: MutableMap<String, String>) {
        preferences.putStringMap(SESSION_TO_VISITOR_MAP, value).apply()
    }

    override fun readSessionToVisitorMap(): MutableMap<String, String>? = preferences.getStringMap(SESSION_TO_VISITOR_MAP)?.toMutableMap()

    // API

    override fun writeSdkSettingKey(value: String) {
        preferences.putString(SDK_SETTING_KEY, value).apply()
    }

    override fun readSdkSettingKey(): String? = preferences.getString(SDK_SETTING_KEY)

    override fun writeJobIdTable(value: String) {
        preferences.putString(JOB_ID_TABLE, value).apply()
    }

    override fun readJobIdTable(): String? = preferences.getString(JOB_ID_TABLE)

    override fun writeJobIdTableLastNumber(value: Int) {
        preferences.putInt(JOB_ID_TABLE_LAST_NUMBER, value).apply()
    }

    override fun readJobIdTableLastNumber() = preferences.getInt(JOB_ID_TABLE_LAST_NUMBER)

    // SDK experimental

    override fun writeSdkVideoSize(value: String) {
        preferences.putString(SDK_VIDEO_SIZE, value).apply()
    }

    override fun readSdkVideoSize(): String? = preferences.getString(SDK_VIDEO_SIZE)

    // Server side config

    override fun writeServerFrameRate(value: Int) {
        preferences.putInt(SERVER_FRAMERATE, value).apply()
    }

    override fun deleteServerFrameRate() {
        preferences.remove(SERVER_FRAMERATE).apply()
    }

    override fun readServerFrameRate(): Int? = preferences.getInt(SERVER_FRAMERATE)

    override fun writeServerBitRate(value: Int) {
        preferences.putInt(SERVER_BITRATE, value).apply()
    }

    override fun deleteServerBitRate() {
        preferences.remove(SERVER_BITRATE).apply()
    }

    override fun readServerBitRate(): Int? = preferences.getInt(SERVER_BITRATE)

    override fun writeServerIsAllowedRecording(value: Boolean) {
        preferences.putBoolean(SERVER_IS_ALLOWED_RECORDING, value).apply()
    }

    override fun deleteServerIsAllowedRecording() {
        preferences.remove(SERVER_IS_ALLOWED_RECORDING).apply()
    }

    override fun readServerIsAllowedRecording(): Boolean? = preferences.getBoolean(SERVER_IS_ALLOWED_RECORDING)

    override fun writeServerIsSensitive(value: Boolean) {
        preferences.putBoolean(SERVER_IS_SENSITIVE, value).apply()
    }

    override fun deleteServerIsSensitive() {
        preferences.remove(SERVER_IS_SENSITIVE).apply()
    }

    override fun readServerIsSensitive(): Boolean? = preferences.getBoolean(SERVER_IS_SENSITIVE)

    override fun writeServerMobileData(value: Boolean) {
        preferences.putBoolean(SERVER_MOBILE_DATA, value).apply()
    }

    override fun deleteServerMobileData() {
        preferences.remove(SERVER_MOBILE_DATA).apply()
    }

    override fun readServerMobileData(): Boolean? = preferences.getBoolean(SERVER_MOBILE_DATA)

    override fun writeServerMaxRecordDuration(value: Int) {
        preferences.putInt(SERVER_MAX_RECORD_DURATION, value).apply()
    }

    override fun deleteServerMaxRecordDuration() {
        preferences.remove(SERVER_MAX_RECORD_DURATION).apply()
    }

    override fun readServerMaxRecordDuration(): Int? = preferences.getInt(SERVER_MAX_RECORD_DURATION)

    override fun writeMaxVideoHeight(value: Int) {
        preferences.putInt(SERVER_MAX_VIDEO_HEIGHT, value).apply()
    }

    override fun deleteMaxVideoHeight() {
        preferences.remove(SERVER_MAX_VIDEO_HEIGHT).apply()
    }

    override fun readMaxVideoHeight(): Int? = preferences.getInt(SERVER_MAX_VIDEO_HEIGHT)

    override fun writeServerMaxSessionDuration(value: Long) {
        preferences.putLong(SERVER_MAX_SESSION_DURATION, value).apply()
    }

    override fun deleteServerMaxSessionDuration() {
        preferences.remove(SERVER_MAX_SESSION_DURATION).apply()
    }

    override fun readServerMaxSessionDuration(): Long? = preferences.getLong(SERVER_MAX_SESSION_DURATION)

    override fun writeServerSessionTimeout(value: Long) {
        preferences.putLong(SERVER_SESSION_TIMEOUT, value).apply()
    }

    override fun deleteServerSessionTimeout() {
        preferences.remove(SERVER_SESSION_TIMEOUT).apply()
    }

    override fun readServerSessionTimeout(): Long? = preferences.getLong(SERVER_SESSION_TIMEOUT)

    override fun writeServerRecordNetwork(value: Boolean) {
        preferences.putBoolean(SERVER_RECORD_NETWORK, value).apply()
    }

    override fun deleteServerRecordNetwork() {
        preferences.remove(SERVER_RECORD_NETWORK).apply()
    }

    override fun readServerRecordNetwork(): Boolean? = preferences.getBoolean(SERVER_RECORD_NETWORK)

    override fun writeServerSessionUrlPattern(value: String) {
        preferences.putString(SERVER_SESSION_URL_PATTERN, value).apply()
    }

    override fun deleteServerSessionUrlPattern() {
        preferences.remove(SERVER_SESSION_URL_PATTERN).apply()
    }

    override fun readServerSessionUrlPattern(): String? = preferences.getString(SERVER_SESSION_URL_PATTERN)

    override fun writeServerVisitorUrlPattern(value: String) {
        preferences.putString(SERVER_VISITOR_URL_PATTERN, value).apply()
    }

    override fun deleteServerVisitorUrlPattern() {
        preferences.remove(SERVER_VISITOR_URL_PATTERN).apply()
    }

    override fun readServerVisitorUrlPattern(): String? = preferences.getString(SERVER_VISITOR_URL_PATTERN)

    override fun writeServerInternalRenderingMode(value: String) {
        preferences.putString(SERVER_INTERNAL_RENDERING_MODE, value).apply()
    }

    override fun deleteServerInternalRenderingMode() {
        preferences.remove(SERVER_INTERNAL_RENDERING_MODE).apply()
    }

    override fun readServerInternalRenderingMode(): String? = preferences.getString(SERVER_INTERNAL_RENDERING_MODE)

    // Application time info timestamps

    override fun writeLastApplicationSettleTimestamp(value: Long) {
        preferences.putLong(LAST_APPLICATION_SETTLE_TIMESTAMP, value).apply()
    }

    override fun deleteLastApplicationSettleTimestamp() {
        preferences.remove(LAST_APPLICATION_SETTLE_TIMESTAMP).apply()
    }

    override fun readLastApplicationSettleTimestamp(): Long? = preferences.getLong(LAST_APPLICATION_SETTLE_TIMESTAMP)

    override fun writeApplicationDurationInBackground(value: Long) {
        preferences.putLong(APPLICATION_DURATION_IN_BACKGROUND, value).apply()
    }

    override fun deleteApplicationDurationInBackground() {
        preferences.remove(APPLICATION_DURATION_IN_BACKGROUND).apply()
    }

    override fun readApplicationDurationInBackground(): Long? = preferences.getLong(APPLICATION_DURATION_IN_BACKGROUND)

    //endregion

    companion object {
        private const val TAG = "Storage"

        /**
         * If storage model changes this version needs to be changed. This will ensure data consistency.
         * The storage will wipe all the legacy data (older version than this one).
         */
        internal const val VERSION = 2

        private var storage: Storage? = null

        fun attach(context: Context): Storage {
            Logger.privateV(LogAspect.STORAGE, TAG, { "attach(): Storage attached." })
            return storage ?: Storage(context).also { storage = it }
        }

        const val LAST_STORAGE_DIR_PATH = "LAST_STORAGE_DIR_PATH"
        const val LAST_VISITOR_ID = "LAST_VISITOR_ID"
        const val SESSION_TO_VISITOR_MAP = "SESSION_TO_VISITOR_MAP"

        // API
        const val SDK_SETTING_KEY = "SDK_SETTING_KEY"

        // JOB Id mapping
        const val JOB_ID_TABLE = "JOB_ID_TABLE"
        const val JOB_ID_TABLE_LAST_NUMBER = "JOB_ID_TABLE_LAST_NUMBER"

        // SDK experimental
        const val SDK_VIDEO_SIZE = "SDK_VIDEO_SIZE"

        // Server side config
        const val SERVER_FRAMERATE = "LAST_CHECK_FRAMERATE"
        const val SERVER_BITRATE = "LAST_CHECK_BITRATE"
        const val SERVER_IS_ALLOWED_RECORDING = "SERVER_IS_ALLOWED_RECORDING"
        const val SERVER_IS_SENSITIVE = "SERVER_IS_SENSITIVE"
        const val SERVER_MOBILE_DATA = "SERVER_MOBILE_DATA"
        const val SERVER_MAX_RECORD_DURATION = "SERVER_MAX_RECORD_DURATION"
        const val SERVER_MAX_VIDEO_HEIGHT = "SERVER_MAX_VIDEO_HEIGHT"
        const val SERVER_MAX_SESSION_DURATION = "SERVER_MAX_SESSION_DURATION"
        const val SERVER_SESSION_TIMEOUT = "SERVER_SESSION_TIMEOUT"
        const val SERVER_RECORD_NETWORK = "SERVER_RECORD_NETWORK"
        const val SERVER_SESSION_URL_PATTERN = "SERVER_SESSION_URL_PATTERN"
        const val SERVER_VISITOR_URL_PATTERN = "SERVER_VISITOR_URL_PATTERN"
        const val SERVER_INTERNAL_RENDERING_MODE = "SERVER_INTERNAL_RENDERING_MODE"

        // Application time info timestamps
        const val LAST_APPLICATION_SETTLE_TIMESTAMP = "LAST_APPLICATION_SETTLE_TIMESTAMP"
        const val APPLICATION_DURATION_IN_BACKGROUND = "APPLICATION_DURATION_IN_BACKGROUND"
    }
}
