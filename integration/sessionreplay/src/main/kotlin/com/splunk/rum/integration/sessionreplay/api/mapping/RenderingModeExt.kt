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

import com.splunk.android.instrumentation.recording.core.api.RenderingMode as CommonRenderingMode
import com.splunk.rum.integration.sessionreplay.api.RenderingMode

internal fun RenderingMode.toCommon(): CommonRenderingMode = when (this) {
    RenderingMode.NATIVE -> CommonRenderingMode.NATIVE
    RenderingMode.WIREFRAME_ONLY -> CommonRenderingMode.WIREFRAME_ONLY
}

internal fun CommonRenderingMode.toSplunk(): RenderingMode = when (this) {
    CommonRenderingMode.NATIVE -> RenderingMode.NATIVE
    CommonRenderingMode.WIREFRAME_ONLY -> RenderingMode.WIREFRAME_ONLY
}
