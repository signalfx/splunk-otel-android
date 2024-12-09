package com.smartlook.sdk.common.id

import java.security.SecureRandom
import java.util.Random
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln

object NanoId {

    private val DEFAULT_NUMBER_GENERATOR = SecureRandom()
    private const val DEFAULT_ALPHABET = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DEFAULT_SIZE = 21

    fun generate(
        random: Random = DEFAULT_NUMBER_GENERATOR,
        alphabet: String = DEFAULT_ALPHABET,
        size: Int = DEFAULT_SIZE
    ): String {

        val arrayAlphabet = alphabet.toCharArray()

        require(!(alphabet.isEmpty() || arrayAlphabet.size >= 256)) { "alphabet must contain between 1 and 255 symbols." }
        require(size > 0) { "size must be greater than zero." }

        val mask = (2 shl floor(ln((arrayAlphabet.size - 1).toDouble()) / ln(2.0)).toInt()) - 1
        val step = ceil(1.6 * mask * size / arrayAlphabet.size).toInt()

        val idBuilder = StringBuilder()
        while (true) {
            val bytes = ByteArray(step)
            random.nextBytes(bytes)
            for (i in 0 until step) {
                val alphabetIndex: Int = bytes[i].toInt() and mask
                if (alphabetIndex < arrayAlphabet.size) {
                    idBuilder.append(alphabet[alphabetIndex])
                    if (idBuilder.length == size) {
                        return idBuilder.toString()
                    }
                }
            }
        }
    }
}
