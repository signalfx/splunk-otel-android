package com.smartlook.android.common.http.extension

import com.smartlook.android.common.http.model.Query
import java.net.URL
import java.net.URLEncoder

internal fun String.toURL(queries: List<Query> = emptyList()): URL {
    val builder = StringBuilder(this)

    if (queries.isNotEmpty())
        builder.append('?')

    for (i in queries.indices) {
        val query = queries[i]

        builder.append(URLEncoder.encode(query.name)).append('=').append(URLEncoder.encode(query.value))

        if (i != queries.lastIndex)
            builder.append('&')
    }

    return URL(builder.toString())
}
