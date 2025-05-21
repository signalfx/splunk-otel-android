package com.splunk.rum.integration.agent.api.attributes

import com.splunk.rum.integration.agent.api.SplunkRumBuilder
import io.opentelemetry.android.export.SpanDataModifier
import io.opentelemetry.api.common.AttributeKey

/**
 * This class hold [AttributeKey]s for standard RUM-related attributes that are not in the
 * OpenTelemetry [io.opentelemetry.semconv.SemanticAttributes] definitions.
 */
@Deprecated("This class will be removed in a future release")
object StandardAttributes {

    /**
     * The version of your app. Useful for adding to global attributes.
     *
     * @see SplunkRumBuilder.setGlobalAttributes
     */
    @JvmStatic
    val APP_VERSION: AttributeKey<String> = AttributeKey.stringKey("app.version")

    /**
     * The build type of your app (typically one of debug or release). Useful for adding to global attributes.
     *
     * @see SplunkRumBuilder.setGlobalAttributes
     */
    @JvmStatic
    val APP_BUILD_TYPE: AttributeKey<String> = AttributeKey.stringKey("app.build.type")

    /**
     * Full HTTP client request URL in the form `scheme://host[:port]/path?query[#fragment]`.
     * Useful for span data filtering with the [SpanDataModifier].
     *
     * @see SemanticAttributes.HTTP_URL
     */
    @JvmStatic
    val HTTP_URL: AttributeKey<String> = AttributeKey.stringKey("http.url")

    @JvmStatic
    val PREVIOUS_SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rum.previous_session_id")

    @JvmStatic
    val SESSION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("splunk.rumSessionId")
}
