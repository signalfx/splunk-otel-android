package com.splunk.app.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.cisco.android.common.utils.runOnUiThread
import com.splunk.app.R

object CommonUtils {
    fun showDoneToast(context: Context?, message: String) {
        if (context == null) {
            Log.e("CommonUtils", "Context is null while attempting to show a toast.")
            return
        }
        runOnUiThread {
            Toast.makeText(context, context.getString(R.string.http_toast, message), Toast.LENGTH_SHORT).show()
        }
    }
}
