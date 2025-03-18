package com.splunk.rum.integration.agent.api.attributes.extension

import com.splunk.rum.integration.agent.api.attributes.GlobalAttributes
import com.splunk.rum.integration.agent.api.SplunkRum

/**
 * Extension property to access the [GlobalAttributes] instance via [SplunkRum].
 */
@Suppress("UnusedReceiverParameter")
val SplunkRum.globalAttributes: GlobalAttributes
    get() = GlobalAttributes.instance