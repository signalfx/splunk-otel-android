/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.mappingfile.plugin.utils

import org.gradle.api.logging.Logger

class SplunkLogger(private val gradleLogger: Logger) {

    companion object {
        private const val PREFIX = "Splunk RUM"
    }

    fun info(tag: String, message: String) {
        gradleLogger.info("$PREFIX: [$tag] $message")
    }

    fun warn(tag: String, message: String) {
        gradleLogger.warn("$PREFIX: [$tag] $message")
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            gradleLogger.error("$PREFIX: [$tag] $message", throwable)
        } else {
            gradleLogger.error("$PREFIX: [$tag] $message")
        }
    }

    fun debug(tag: String, message: String) {
        gradleLogger.debug("$PREFIX: [$tag] $message")
    }

    fun lifecycle(tag: String, message: String) {
        gradleLogger.lifecycle("$PREFIX: [$tag] $message")
    }
}
