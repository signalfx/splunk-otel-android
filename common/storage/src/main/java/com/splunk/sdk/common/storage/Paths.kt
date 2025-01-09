package com.splunk.sdk.common.storage

import android.content.Context
import android.os.Build
import com.splunk.sdk.common.storage.extensions.createNewFileOnPath
import com.splunk.sdk.common.storage.extensions.formatChildDir
import com.splunk.sdk.common.storage.extensions.formatChildFile
import com.splunk.sdk.common.utils.runOnAndroidAtLeast
import java.io.File

/**
 * SDK storage structure:
 * smartlook/
 * └─<STORAGE_VERSION>/
 *   ├─otel-data/
 *   │ ├─span/
 *   │ │ └─data.dat
 *   │ ├─log/
 *   │   └─data.dat
 *   ├─identification/
 *   │ └─<visitorId>/
 *   │   └─identification.dat
 *   └─sessions/
 *     └─<session_id>
 *         └─records/
 *            └─<record_index>/
 *              ├──metrics.dat
 *              ├──record.dat
 *              ├──wireframe.dat
 *              └──video/
 *                ├──<frame_number>.jpg
 *                ├──video.mp4
 *                └──config.dat
 */
internal class Paths(context: Context) {

    val systemRootDir: File = runOnAndroidAtLeast(Build.VERSION_CODES.LOLLIPOP) { context.noBackupFilesDir } ?: throw IllegalStateException("Storage is not supported on this Android version")

    /**
     * Root folder.
     */
    val rootDir: File by lazy { systemRootDir.formatChildDir("smartlook_${Storage.VERSION}").apply { mkdirs() } }

    /**
     * Preferences directory/file.
     */
    private val preferencesDir: File by lazy { rootDir.formatChildDir("preferences").apply { mkdirs() } }
    val preferencesFile: File by lazy { preferencesDir.formatChildFile("preferences.1.dat").createNewFileOnPath() }

    /**
     * Identification folder/file.
     */
    fun identificationDir(visitorId: String): File = rootDir.formatChildDir("identification${File.separator}$visitorId").apply { mkdirs() }
    fun identificationFile(visitorId: String) = identificationDir(visitorId).formatChildFile("identification.txt")

    /**
     * Sessions folder. Specific session folders.
     */
    val sessionsDir = rootDir.formatChildDir("sessions")
    fun sessionDir(sessionId: String) = sessionsDir.formatChildDir(sessionId)

    /**
     * Records folder. Specific record folders. Record file.
     */
    fun recordsDir(sessionId: String): File = sessionDir(sessionId).formatChildDir("records").apply { mkdirs() }
    fun recordDir(sessionId: String, recordIndex: Int): File = recordsDir(sessionId).formatChildDir(recordIndex.toString()).apply { mkdirs() }
    fun recordFile(sessionId: String, recordIndex: Int) = recordDir(sessionId, recordIndex).formatChildFile("record.txt")

    /**
     * Metrics file stored inside a record dir.
     */
    fun metricsFile(sessionId: String, recordIndex: Int) = recordDir(sessionId, recordIndex).formatChildFile("metrics.txt")

    /**
     * Video images folder. Specific record video folders.
     */
    fun videoImageDir(sessionId: String, recordIndex: Int): File = recordDir(sessionId, recordIndex).formatChildDir("video").apply { mkdirs() }

    /**
     * Video images and video files.
     */
    fun videoImageFile(sessionId: String, recordIndex: Int, frameNumber: Int) = videoImageDir(sessionId, recordIndex).formatChildFile("$frameNumber.jpg")
    fun videoFile(sessionId: String, recordIndex: Int) = videoImageDir(sessionId, recordIndex).formatChildFile("video.mp4")
    fun videoConfigFile(sessionId: String, recordIndex: Int) = videoImageDir(sessionId, recordIndex).formatChildFile("config.txt")

    /**
     * Wireframe file stored inside a record video dir.
     */
    fun wireframeFile(sessionId: String, recordIndex: Int) = recordDir(sessionId, recordIndex).formatChildFile("wireframe.txt")

    val otelDir: File by lazy { rootDir.formatChildDir("otel-data").apply { mkdirs() } }
    val otelSpanDir: File by lazy { otelDir.formatChildDir("span").apply { mkdirs() } }
    val otelLogDir: File by lazy { otelDir.formatChildDir("log").apply { mkdirs() } }

    fun otelSpanFile(id: String) = otelSpanDir.formatChildFile("$id.dat")
    fun otelLogFile(id: String) = otelLogDir.formatChildFile("$id.dat")
}
