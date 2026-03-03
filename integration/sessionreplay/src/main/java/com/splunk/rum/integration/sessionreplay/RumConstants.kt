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