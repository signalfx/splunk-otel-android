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

package com.splunk.rum.agent.common.utils

import com.splunk.rum.common.logger.Logger
/**
 * Serializes construction of instrumenters that temporarily change the process-wide OTel SPI
 * loader.
 *
 * The caller supplies the loader override, instrumenter build, and reset action. The empty loader
 * override is short-lived, but the reset action permanently sets the OTel default loader because
 * ServiceLoaderUtil does not expose a getter for any prior custom process-wide loader. If the
 * optional loader override is unavailable in the resolved OTel instrumentation API, the normal
 * build is used instead.
 */
object InstrumenterBuildUtils {
    private val instrumenterBuildLock = Any()

    @JvmStatic
    fun <T> synchronizedInstrumenterBuild(
        configureSpiLookup: () -> Unit,
        restoreSpiLookup: () -> Unit,
        build: () -> T,
        fallbackBuild: () -> T
    ): T = synchronized(instrumenterBuildLock) {
        try {
            configureSpiLookup()
        } catch (error: LinkageError) {
            Logger.w(
                TAG,
                "Unable to disable the instrumenter SPI lookup; using the normal instrumenter build " +
                    "(${error.javaClass.simpleName})"
            )
            return@synchronized fallbackBuild()
        }

        try {
            build()
        } finally {
            restoreSpiLookup()
        }
    }

    private const val TAG = "InstrumenterBuildUtils"
}
