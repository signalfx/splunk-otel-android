package com.splunk.rum.integration.agent.api.attributes.extension

import com.splunk.rum.integration.agent.api.SplunkRUMAgent
import com.splunk.rum.integration.agent.api.attributes.GlobalAttributes

/**
 * Extension property to access the [GlobalAttributes] instance via [SplunkRUMAgent].
 */
@Suppress("UnusedReceiverParameter")
val SplunkRUMAgent.globalAttributes: GlobalAttributes
    get() = GlobalAttributes.instance
