package com.splunk.rum.integration.navigation.extension

import com.splunk.rum.integration.agent.api.SplunkRUMAgent
import com.splunk.rum.integration.navigation.Navigation

val SplunkRUMAgent.navigation: Navigation
    get() = Navigation
