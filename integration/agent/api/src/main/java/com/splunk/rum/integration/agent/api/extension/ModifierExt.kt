package com.splunk.rum.integration.agent.api.extension

import androidx.compose.ui.Modifier
import com.splunk.rum.integration.agent.internal.identification.ComposeElementIdentification

fun Modifier.splunkRum(id: String? = null, isSensitive: Boolean? = null, positionInList: Int? = null): Modifier {
    return ComposeElementIdentification.resolveChain(this, id, isSensitive, positionInList)
}
