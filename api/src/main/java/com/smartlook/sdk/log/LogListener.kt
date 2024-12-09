package com.smartlook.sdk.log

/**
 * Listener for log.
 *
 * @see Log
 */
interface LogListener {

    /**
     * Called when new log message is available.
     *
     * @param severity can be on of VERBOSE, DEBUG, INFO, WARN or ERROR.
     */
    fun onLog(@LogAspect.Aspect aspect: Long, severity: String?, tag: String, message: String)
}
