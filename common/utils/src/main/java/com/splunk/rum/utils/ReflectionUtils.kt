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

package com.splunk.rum.utils

/**
 * @deprecated
 * Hackish support for legacy API mapping via reflection.
 *
 * This utility uses reflection to invoke methods on singleton instances or companion objects.
 * It is fragile and should only be used as a last resort for legacy APIs.
 * Prefer direct calls to stable, public APIs whenever possible.
 */
@Deprecated("Hackish support for legacy API mapping")
object ReflectionUtils {

    fun <T> invokeOnClassInstance(
        className: String,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any?>
    ): T? = try {
        val clazz = Class.forName(className)
        val instance = clazz.getField("INSTANCE").get(null)
        val method = clazz.getDeclaredMethod(methodName, *parameterTypes)
        @Suppress("UNCHECKED_CAST")
        method.invoke(instance, *args) as? T
    } catch (_: Exception) {
        null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> invokeOnCompanionInstance(
        className: String,
        methodName: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any?>
    ): T? = try {
        val clazz = Class.forName(className)

        val instance = try {
            clazz.getField("INSTANCE").get(null)
        } catch (_: NoSuchFieldException) {
            val companion = clazz.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
            val instanceMethod = companion.javaClass.getDeclaredMethod("getInstance").apply { isAccessible = true }
            instanceMethod.invoke(companion)
        }

        clazz.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
            .invoke(instance, *args) as? T
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
