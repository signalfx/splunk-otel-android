/*
 * Copyright 2024 Splunk Inc.
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

package com.smartlook.app.lib

import android.app.Instrumentation
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Instrumentation
 */
val instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()

/**
 * The target Context
 */
val targetContext: Context = instrumentation.targetContext

/**
 * A resource type enum to specify what kind of resource you want to interact with.
 */
enum class ResourceType {
    RESOURCE_ID, RESOURCE_STRING
}

/**
 * This will take an Int and return a resource type or throw an error if the resource type doesn't
 * match.
 */
val Int.resType: ResourceType
    get() = when (targetContext.resources.getResourceTypeName(this)) {
        "id" -> ResourceType.RESOURCE_ID
        "string" -> ResourceType.RESOURCE_STRING
        else -> throw NoSuchElementException()
    }
