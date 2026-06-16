package com.splunk.rum.integration.interactions

import com.splunk.android.instrumentation.recording.interactions.model.Interaction

internal object XpathBuilder {

    private const val PREFIX = "//Scene/Window/"
    private const val USER_ID_PREFIX = "userid_"

    /**
     * Builds a simple XPath-like string from the interaction's target path by
     * concatenating each element's view typename, appending a positional suffix
     * (e.g., `[3]`) when a position in list is provided, adding user-provided
     * ids (prefixed with `userid_`) as `[@id="..."]` (without the prefix), and
     * joining segments with `/`. Prepends `//Scene/Window/` when a path exists.
     * Returns an empty string when the path is absent.
     */
    // TODO Compose neds to add userid_ prefix to the ids if the user specifies id. Currently it only works for XML.
    fun build(interactions: Interaction.Targetable): String {
        val xpath = interactions.targetElementPath
            ?.joinToString(separator = "/") { element ->
                val positionSuffix = element.positionInList?.let { "[$it]" }.orEmpty()

                val userId = element.view.id
                    ?.takeIf { it.startsWith(USER_ID_PREFIX) }
                    ?.removePrefix(USER_ID_PREFIX)

                val idSuffix = userId
                    ?.let { """[@id="$it"]""" }
                    .orEmpty()

                "${element.view.typename}$positionSuffix$idSuffix"
            }
            .orEmpty()

        return if (xpath.isNotEmpty()) {
            "$PREFIX$xpath"
        } else {
            ""
        }
    }
}
