package com.smartlook.android.common.http.extension

import com.smartlook.android.common.http.model.Header

internal fun Map<String?, List<String>>.toHeaders(): List<Header> {
    val headers = ArrayList<Header>(size)

    for ((name, values) in this)
        if (name != null)
            for (value in values)
                headers += Header(name, value)

    return headers
}
