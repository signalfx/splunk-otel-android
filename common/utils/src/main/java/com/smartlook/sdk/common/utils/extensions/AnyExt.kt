package com.smartlook.sdk.common.utils.extensions

import java.lang.reflect.Field
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T : Any> Any.invoke(methodName: String, vararg paramsPairs: Pair<Any?, KClass<*>>): T? {
    val classes = paramsPairs.map { it.second.java }.toTypedArray()
    val params = paramsPairs.map { it.first }.toTypedArray()
    var superclass = this::class.java

    do {
        try {
            val method = superclass.getDeclaredMethod(methodName, *classes)
            method.isAccessible = true

            return method.invoke(this, *params) as? T
        } catch (e: NoSuchMethodException) {
            superclass = superclass.superclass ?: break
        }
    } while (true)

    throw NoSuchMethodException("Unable to invoke '${this::class.java.name}.$methodName(${classes.joinToString(", ") { it.name }})'")
}

fun <T : Any> Any.get(fieldName: String): T? {
    val field = findField(fieldName)
    return get(field)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> Any.get(field: Field): T? {
    field.makeReadable()
    return field.get(this) as? T
}

fun <T : Any> Any.set(fieldName: String, value: T?) {
    val field = findField(fieldName)
    set(field, value)
}

fun <T : Any> Any.set(field: Field, value: T?) {
    field.makeWritable()
    field.set(this, value)
}

fun Any.findField(name: String): Field {
    return this::class.findField(name)
}
