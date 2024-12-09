package com.smartlook.sdk.common.logger

import android.util.Log
import com.smartlook.sdk.common.logger.extension.toSeverityString
import com.smartlook.sdk.log.LogAspect
import com.smartlook.sdk.log.LogListener
import org.json.JSONObject
import java.lang.ref.WeakReference

object Logger {

    /**
     * Internal logs are handled in smartlooksdk module.
     */
    var internalLogListeners: MutableList<InternalLogListener> = mutableListOf()

    @LogAspect.Aspect
    var allowedLogAspects: Long = LogAspect.API
    var listeners: MutableList<WeakReference<LogListener>> = mutableListOf()
    private var minimalLogSeverity: Int = Log.VERBOSE
    private val logPrinter by lazy { LogPrinter() }

    //region Public logs

    fun v(aspect: Long, tag: String, message: () -> String) =
        log(aspect, false, Log.VERBOSE, tag, message)

    fun d(aspect: Long, tag: String, message: () -> String) =
        log(aspect, false, Log.DEBUG, tag, message)

    fun i(aspect: Long, tag: String, message: () -> String) =
        log(aspect, false, Log.INFO, tag, message)

    fun w(aspect: Long, tag: String, message: () -> String) =
        log(aspect, false, Log.WARN, tag, message)

    fun e(aspect: Long, tag: String, message: () -> String) =
        log(aspect, false, Log.ERROR, tag, message)

    //endregion

    //region Private logs

    fun privateV(aspect: Long, tag: String, message: () -> String, publicMessage: (() -> String)? = null) =
        log(aspect, true, Log.VERBOSE, tag, message, publicMessage)

    fun privateD(aspect: Long, tag: String, message: () -> String, publicMessage: (() -> String)? = null) =
        log(aspect, true, Log.DEBUG, tag, message, publicMessage)

    fun privateI(aspect: Long, tag: String, message: () -> String, publicMessage: (() -> String)? = null) =
        log(aspect, true, Log.INFO, tag, message, publicMessage)

    fun privateW(aspect: Long, tag: String, message: () -> String, publicMessage: (() -> String)? = null) =
        log(aspect, true, Log.WARN, tag, message, publicMessage)

    fun privateE(aspect: Long, tag: String, message: () -> String, publicMessage: (() -> String)? = null) =
        log(aspect, true, Log.ERROR, tag, message, publicMessage)

    //endregion

    private fun log(
        aspect: Long,
        isPrivate: Boolean,
        severity: Int,
        tag: String,
        message: () -> String,
        publicMessage: (() -> String)? = null
    ) {
        val logMessage = when (shouldLog(aspect, isPrivate, severity)) {
            LogCheck.ALLOWED -> message.invoke()
            LogCheck.ONLY_PUBLIC_MESSAGE -> publicMessage?.invoke()
            LogCheck.NOT_ALLOWED -> null
        }

        if (logMessage != null) {
            if (listeners.any { it.get() != null }) {
                listeners.forEach { it.get()?.onLog(aspect, severity.toSeverityString(), tag, logMessage) }
            } else {
                logPrinter.print(severity, aspect, tag, logMessage)
            }
        }
    }

    /**
     * On [BuildConfig.DEBUG] only logs with severity equal or above [minimalLogSeverity] are logged.
     * On release version of SDK only non-private logs with allowed aspect and severity equal or above
     * minimal one can be logged with original message. If alternative private log message is provided
     * even private logs can be logged (other criteria must be met).
     */
    private fun shouldLog(@LogAspect.Aspect aspect: Long, privateLog: Boolean, severity: Int): LogCheck {
        return when {
            severity < minimalLogSeverity -> LogCheck.NOT_ALLOWED
            allowedLogAspects and aspect == aspect -> {
                if (privateLog && !BuildConfig.DEBUG) LogCheck.ONLY_PUBLIC_MESSAGE else LogCheck.ALLOWED
            }
            else ->
                LogCheck.NOT_ALLOWED
        }
    }

    interface InternalLogListener {
        fun onLog(
            severity: Int,
            id: String,
            key: String,
            message: String,
            context: JSONObject?,
            tags: Map<String, String>?
        )
    }

    enum class LogCheck {
        ALLOWED,
        ONLY_PUBLIC_MESSAGE,
        NOT_ALLOWED
    }
}
