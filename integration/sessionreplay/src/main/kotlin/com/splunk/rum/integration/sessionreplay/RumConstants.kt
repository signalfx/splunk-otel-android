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

package com.splunk.rum.integration.sessionreplay

import io.opentelemetry.api.common.AttributeKey

internal object RumConstants {

    const val COMPONENT_SESSION_REPLAY = "session.replay"
    const val SESSION_REPLAY_DATA_EVENT_NAME = "session_replay_data"
    const val SESSION_REPLAY_IS_RECORDING_EVENT_NAME = "splunk.sessionReplay.isRecording"
    const val SESSION_REPLAY_PROVIDER = "splunk"

    // Attribute key
    val SESSION_REPLAY_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.sessionReplay")
    val SESSION_REPLAY_TOTAL_CHUNKS_KEY: AttributeKey<Double> = AttributeKey.doubleKey("rr-web.total-chunks")
    val SESSION_REPLAY_CHUNK_KEY: AttributeKey<Double> = AttributeKey.doubleKey("rr-web.chunk")
    val SESSION_REPLAY_EVENT_INDEX_KEY: AttributeKey<Long> = AttributeKey.longKey("rr-web.event")
    val SESSION_REPLAY_OFFSET_KEY: AttributeKey<Double> = AttributeKey.doubleKey("rr-web.offset")
    val SESSION_REPLAY_SEGMENT_METADATA_KEY: AttributeKey<String> = AttributeKey.stringKey("segmentMetadata")
}
