package com.splunk.sdk.common.storage

import android.graphics.Bitmap
import com.splunk.sdk.common.storage.preferences.IPreferences
import java.io.File

interface IStorage {

    val freeSpace: Long
    val consistentDirPath: String
    val isSessionStorageFull: Boolean
    val preferences: IPreferences

    fun writeRecord(sessionId: String, recordIndex: Int, recordJson: String): Boolean
    fun readRecord(sessionId: String, recordIndex: Int): String?
    fun deleteRecord(sessionId: String, recordIndex: Int): Boolean

    fun getSessionIds(): List<String>
    fun getRecordIndexes(sessionId: String): List<Int>
    fun hasSessionData(sessionId: String): Boolean
    fun deleteSession(sessionId: String): Boolean
    fun deleteOldestSession(excludeSessionId: String): String?

    fun getWireframeFile(sessionName: String, recordIndex: Int): File
    fun isWireframeFileAvailable(sessionId: String, recordIndex: Int): Boolean

    fun writeWireframe(sessionId: String, recordIndex: Int, wireframe: ByteArray): Boolean
    fun writeMetrics(sessionId: String, recordIndex: Int, metrics: String): Boolean
    fun readMetrics(sessionId: String, recordIndex: Int): String?

    fun getVideoImageDir(sessionId: String, recordIndex: Int): File
    fun writeVideoConfig(sessionId: String, recordIndex: Int, config: String): Boolean
    fun readVideoConfig(sessionId: String, recordIndex: Int): String?
    fun writeVideoFrame(sessionId: String, recordIndex: Int, frameIndex: Int, frame: Bitmap, format: Bitmap.CompressFormat, quality: Int): Boolean
    fun deleteAllVideoFrames(sessionId: String, recordIndex: Int): Boolean
    fun createVideoFile(sessionId: String, recordIndex: Int): File
    fun getVideoFile(sessionId: String, recordIndex: Int): File
    fun isVideoFileAvailable(sessionId: String, recordIndex: Int): Boolean

    fun writeIdentification(visitorId: String, identification: String): Boolean
    fun readIdentification(visitorId: String): String?
    fun deleteIdentification(visitorId: String): Boolean
    fun deleteInconsistentPath(path: String)

    fun getOtelSpanDataDir(): File
    fun getOtelLogDataDir(): File
    fun createOtelSpanDataFile(id: String): File
    fun getOtelSpanDataFile(id: String): File
    fun createOtelLogDataFile(id: String): File
    fun getOtelLogDataFile(id: String): File

    fun invalidateAllPreferences()

    fun writeLastStorageDirPath(value: String)
    fun deleteLastStorageDirPath()
    fun readLastStorageDirPath(): String?

    fun writeLastVisitorId(value: String)
    fun deleteLastVisitorId()
    fun readLastVisitorId(): String?

    fun writeSessionToVisitorMap(value: MutableMap<String, String>)
    fun readSessionToVisitorMap(): MutableMap<String, String>?

    fun writeSdkSettingKey(value: String)
    fun readSdkSettingKey(): String?

    fun writeJobIdTable(value: String)
    fun readJobIdTable(): String?

    fun writeJobIdTableLastNumber(value: Int)
    fun readJobIdTableLastNumber(): Int?

    fun writeSdkVideoSize(value: String)
    fun readSdkVideoSize(): String?

    fun writeServerFrameRate(value: Int)
    fun deleteServerFrameRate()
    fun readServerFrameRate(): Int?

    fun writeServerBitRate(value: Int)
    fun deleteServerBitRate()
    fun readServerBitRate(): Int?

    fun writeServerIsAllowedRecording(value: Boolean)
    fun deleteServerIsAllowedRecording()
    fun readServerIsAllowedRecording(): Boolean?

    fun writeServerIsSensitive(value: Boolean)
    fun deleteServerIsSensitive()
    fun readServerIsSensitive(): Boolean?

    fun writeServerMobileData(value: Boolean)
    fun deleteServerMobileData()
    fun readServerMobileData(): Boolean?

    fun writeServerMaxRecordDuration(value: Int)
    fun deleteServerMaxRecordDuration()
    fun readServerMaxRecordDuration(): Int?

    fun writeMaxVideoHeight(value: Int)
    fun deleteMaxVideoHeight()
    fun readMaxVideoHeight(): Int?

    fun writeServerMaxSessionDuration(value: Long)
    fun deleteServerMaxSessionDuration()
    fun readServerMaxSessionDuration(): Long?

    fun writeServerSessionTimeout(value: Long)
    fun deleteServerSessionTimeout()
    fun readServerSessionTimeout(): Long?

    fun writeServerRecordNetwork(value: Boolean)
    fun deleteServerRecordNetwork()
    fun readServerRecordNetwork(): Boolean?

    fun writeServerSessionUrlPattern(value: String)
    fun deleteServerSessionUrlPattern()
    fun readServerSessionUrlPattern(): String?

    fun writeServerVisitorUrlPattern(value: String)
    fun deleteServerVisitorUrlPattern()
    fun readServerVisitorUrlPattern(): String?

    fun writeServerInternalRenderingMode(value: String)
    fun deleteServerInternalRenderingMode()
    fun readServerInternalRenderingMode(): String?

    fun writeLastApplicationSettleTimestamp(value: Long)
    fun deleteLastApplicationSettleTimestamp()
    fun readLastApplicationSettleTimestamp(): Long?

    fun writeApplicationDurationInBackground(value: Long)
    fun deleteApplicationDurationInBackground()
    fun readApplicationDurationInBackground(): Long?
}
