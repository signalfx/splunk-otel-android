package com.smartlook.sdk.common.utils.extensions

import android.content.Context
import android.os.Build
import com.smartlook.sdk.common.utils.runOnAndroidAtLeast
import java.io.File

private var fragmentSpecialEffectsControllerViewTag: Int? = 0

internal fun Context.getFragmentSpecialEffectsControllerViewTag(): Int? { // androidx.fragment.R.id.special_effects_controller_view_tag
    if (fragmentSpecialEffectsControllerViewTag == 0) {
        val name = "$packageName:id/special_effects_controller_view_tag"
        fragmentSpecialEffectsControllerViewTag = runCatching { resources.getValue(name).resourceId }.getOrNull()
    }

    return fragmentSpecialEffectsControllerViewTag
}

val Context.noBackupFilesDirCompat: File
    get() = runOnAndroidAtLeast(Build.VERSION_CODES.LOLLIPOP) { noBackupFilesDir } ?: cacheDir
