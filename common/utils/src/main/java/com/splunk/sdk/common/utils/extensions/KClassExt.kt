package com.splunk.sdk.common.utils.extensions

import java.lang.reflect.Field
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T> KClass<*>.getStatic(fieldName: String): T? {
    val field = findField(fieldName)
    field.makeReadable()

    return field.get(null) as? T
}

internal fun KClass<*>.findField(name: String): Field {
    var superclass: Class<*> = java

    do {
        try {
            return superclass.getDeclaredField(name)
        } catch (e: NoSuchFieldException) {
            superclass = superclass.superclass ?: break
        }
    } while (true)

    throw NoSuchFieldException("Property '${java.name}.$name' not found")
}
