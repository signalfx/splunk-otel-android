package com.splunk.android.common.http.extension

import com.splunk.android.common.http.model.part.ByteArrayPart
import com.splunk.android.common.http.model.part.ContentPart
import com.splunk.android.common.http.model.part.FilePart
import com.splunk.android.common.http.model.part.Part
import com.splunk.android.common.http.model.part.StringPart
import java.io.FileNotFoundException
import java.io.OutputStream

internal fun OutputStream.write(string: String) {
    write(string.toByteArray())
}

private const val LINE_END = "\r\n"

fun OutputStream.write(parts: List<Part>, boundary: String) {
    for (part in parts) {
        if (part is FilePart && !part.file.exists())
            throw FileNotFoundException("File body part '${part.file.absolutePath}' doesn't exist")

        write("--$boundary$LINE_END")
        write(part.toContentDispositionHeader() + LINE_END)

        if (part is FilePart || part is ByteArrayPart || part is ContentPart)
            write("Content-Transfer-Encoding: binary$LINE_END")

        if (part.contentEncoding != null)
            write("Content-Encoding: ${part.contentEncoding}$LINE_END")

        write("Content-Type: ${part.contentType}$LINE_END")
        write("Content-Length: ${part.getLength()}$LINE_END")
        write(LINE_END)

        when (part) {
            is FilePart ->
                part.file.inputStream().copyTo(this)
            is StringPart ->
                write(part.string)
            is ByteArrayPart ->
                write(part.bytes)
            is ContentPart ->
                part.copyInto(this)
        }

        write(LINE_END)
    }

    write("--$boundary--$LINE_END")
}

private fun Part.toContentDispositionHeader(): String {
    return when (this) {
        is FilePart -> "Content-Disposition: form-data; name=\"$name\"; filename=\"${file.name}\""
        is StringPart, is ByteArrayPart, is ContentPart -> "Content-Disposition: form-data; name=\"$name\""
    }
}
