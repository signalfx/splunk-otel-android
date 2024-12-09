package com.smartlook.sdk.common.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashCalculationUtils {

    // This is the default block size of ext3/ext4 filesystems which android is based on
    private const val BUFFER_SIZE = 32_768

    fun calculateSha256(file: File): String {
        // Reading apk file in chunks at a time
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        val inputStream = FileInputStream(file)

        try {
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
        } finally {
            inputStream.close()
        }

        val hashBytes = digest.digest()

        // Convert byte array to lower case hexadecimal string
        val hexString = StringBuilder()
        for (byte in hashBytes) {
            hexString.append(String.format("%02x", byte))
        }
        return hexString.toString()
    }
}
