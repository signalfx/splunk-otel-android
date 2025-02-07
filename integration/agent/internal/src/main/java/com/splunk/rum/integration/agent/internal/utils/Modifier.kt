package com.splunk.rum.integration.agent.internal.utils

import com.cisco.android.common.utils.extensions.toClass

private val modifierClass = "androidx.compose.ui.draw.DrawModifier".toClass()

fun runIfComposeUiExists(block: () -> Unit) {
    if (modifierClass != null)
        block()
}
