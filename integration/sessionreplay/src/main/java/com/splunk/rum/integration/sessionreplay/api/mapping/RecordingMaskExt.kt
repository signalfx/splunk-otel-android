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

package com.splunk.rum.integration.sessionreplay.api.mapping

import com.cisco.android.instrumentation.recording.core.api.RecordingMask as CommonRecordingMask
import com.splunk.rum.integration.sessionreplay.api.RecordingMask

internal fun RecordingMask.toCommon(): CommonRecordingMask {
    val newElements = elements.map {
        val newType = when (it.type) {
            RecordingMask.Element.Type.COVERING -> CommonRecordingMask.Element.Type.COVERING
            RecordingMask.Element.Type.ERASING -> CommonRecordingMask.Element.Type.ERASING
        }
        CommonRecordingMask.Element(rect = it.rect, type = newType)
    }
    return CommonRecordingMask(newElements)
}

internal fun CommonRecordingMask.toSplunk(): RecordingMask {
    val newElements = elements.map {
        val newType = when (it.type) {
            CommonRecordingMask.Element.Type.COVERING -> RecordingMask.Element.Type.COVERING
            CommonRecordingMask.Element.Type.ERASING -> RecordingMask.Element.Type.ERASING
        }
        RecordingMask.Element(rect = it.rect, type = newType)
    }
    return RecordingMask(newElements)
}
