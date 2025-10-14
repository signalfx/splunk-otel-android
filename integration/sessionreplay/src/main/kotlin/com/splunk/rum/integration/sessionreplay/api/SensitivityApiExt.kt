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

package com.splunk.rum.integration.sessionreplay.api

import android.view.View
import androidx.compose.ui.Modifier
import com.splunk.android.instrumentation.recording.core.api.isSensitive
import com.splunk.android.instrumentation.recording.interactions.compose.PointerInputObserverInjectorModifier
import com.splunk.android.instrumentation.recording.wireframe.canvas.compose.SessionReplayDrawModifier
import kotlin.reflect.KClass

/**
 * Equivalent to [Sensitivity.getViewInstanceSensitivity] and [Sensitivity.setViewInstanceSensitivity].
 */
@get:JvmSynthetic
@set:JvmSynthetic
var View.isSensitive: Boolean?
    get() = isSensitive
    set(value) {
        isSensitive = value
    }

/**
 * Equivalent to [Sensitivity.getViewClassSensitivity] and [Sensitivity.setViewClassSensitivity].
 */
@get:JvmSynthetic
@set:JvmSynthetic
var <T : View> KClass<T>.isSensitive: Boolean?
    get() = isSensitive
    set(value) {
        isSensitive = value
    }

/**
 * Equivalent to [Sensitivity.getViewClassSensitivity] and [Sensitivity.setViewClassSensitivity].
 */
@get:JvmSynthetic
@set:JvmSynthetic
var <T : View> Class<T>.isSensitive: Boolean?
    get() = isSensitive
    set(value) {
        isSensitive = value
    }

/**
 * Session Replay [Modifier] that adds additional info into wireframe.
 */
fun Modifier.sessionReplay(id: String? = null, isSensitive: Boolean? = null, positionInList: Int? = null): Modifier {
    var modifier = then(SessionReplayDrawModifier(id, isSensitive))

    if (id != null) {
        modifier = modifier.then(PointerInputObserverInjectorModifier(id, positionInList))
    }

    return modifier
}
