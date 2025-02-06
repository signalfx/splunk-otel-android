package com.splunk.rum.integration.agent.internal.identification

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import kotlin.reflect.KClass

@SuppressLint("ModifierFactoryExtensionFunction")
object ComposeElementIdentification {

    enum class OrderPriority {
        HIGH, MEDIUM, LOW
    }

    private val chain = ArrayList<Element>()

    fun <T : Modifier> insertModifierIfNeeded(kClass: KClass<T>, orderPriority: OrderPriority, constructor: (id: String?, isSensitive: Boolean?, positionInList: Int?) -> T?) {
        if (chain.any { it.kClass == kClass })
            return

        chain += Element(orderPriority, constructor, kClass)
        chain.sortBy { it.orderPriority.ordinal }
    }

    fun resolveChain(modifier: Modifier, id: String?, isSensitive: Boolean?, positionInList: Int?): Modifier {
        var result = modifier

        for (i in chain.indices) {
            val innerModifier = chain[i].constructor(id, isSensitive, positionInList) ?: continue
            result = result.then(innerModifier)
        }

        return result
    }

    private class Element(
        val orderPriority: OrderPriority,
        val constructor: (id: String?, isSensitive: Boolean?, positionInList: Int?) -> Modifier?,
        val kClass: KClass<out Modifier>
    )
}
