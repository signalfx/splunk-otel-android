package com.splunk.rum.integration.agent.api.attributes

import io.opentelemetry.api.common.Attributes

fun Attributes.toMutableAttributes() = MutableAttributes(this)