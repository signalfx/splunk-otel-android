package com.splunk.rum.integration.interactions

import android.graphics.Point
import android.graphics.Rect
import com.splunk.android.common.utils.Lock
import com.splunk.android.instrumentation.recording.interactions.model.ElementNode
import com.splunk.android.instrumentation.recording.interactions.model.Interaction
import com.splunk.android.instrumentation.recording.wireframe.model.Wireframe
import junit.framework.TestCase.assertEquals
import org.junit.Test

internal class XPathBuilderTest {

    @Test
    fun `returns empty string when path is null`() {
        val interaction = targetable(null)

        val result = XpathBuilder.build(interaction)

        assertEquals("", result)
    }

    @Test
    fun `returns empty string when path is empty`() {
        val interaction = targetable(emptyList())

        val result = XpathBuilder.build(interaction)

        assertEquals("", result)
    }

    @Test
    fun `builds path with typename and prefix`() {
        val interaction = targetable(
            listOf(
                element(view(typename = "Button"))
            )
        )

        val result = XpathBuilder.build(interaction)

        assertEquals("//Scene/Window/Button", result)
    }

    @Test
    fun `appends position before user id`() {
        val interaction = targetable(
            listOf(
                element(view(typename = "Item", id = "userid_custom"), position = 3)
            )
        )

        val result = XpathBuilder.build(interaction)

        assertEquals("//Scene/Window/Item[3][@id=\"custom\"]", result)
    }

    @Test
    fun `ignores non user ids and joins multiple segments`() {
        val interaction = targetable(
            listOf(
                element(view(typename = "ListView", id = "list_1")),
                element(view(typename = "Row", id = "userid_row")),
                element(view(typename = "Text", id = "userid_value"), position = 2)
            )
        )

        val result = XpathBuilder.build(interaction)

        assertEquals("//Scene/Window/ListView/Row[@id=\"row\"]/Text[2][@id=\"value\"]", result)
    }

    private fun targetable(path: List<ElementNode>?) = object : Interaction.Targetable {
        override val targetElementPath: List<ElementNode>? = path
    }

    private fun element(view: Wireframe.Frame.Scene.Window.View, position: Int? = null): ElementNode = ElementNode(
        view = view,
        positionInList = position,
        fragmentTag = null
    )

    private fun view(id: String? = null, typename: String): Wireframe.Frame.Scene.Window.View =
        Wireframe.Frame.Scene.Window.View(
            id = id.orEmpty(),
            name = "",
            rect = Rect(),
            type = Wireframe.Frame.Scene.Window.View.Type.AREA,
            typename = typename,
            hasFocus = false,
            offset = Point(),
            alpha = 1f,
            skeletons = emptyList(),
            foregroundSkeletons = emptyList(),
            subviews = mutableListOf(),
            identity = "",
            isDrawDeterministic = false,
            isSensitive = false,
            subviewsLock = Lock()
        )
}
