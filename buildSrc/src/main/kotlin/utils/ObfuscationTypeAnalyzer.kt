package utils

import com.android.build.gradle.AppExtension
import org.gradle.api.Project

class ObfuscationTypeAnalyzer {
    enum class ObfuscationType(private val stringValue: String) {
        PROGUARD("Proguard"),
        R8("R8"),
        DEXGUARD("Dexguard"),
        NO_OBFUSCATION("No Obfuscation");
        override fun toString() = stringValue
    }

    companion object {
        const val ANDROID_ENABLER8 = "android.enableR8"
    }

    // This is a little awkward. R8 is the default android shrinker, and the customer generally
    // has to go out of their way to disable r8 and use proguard. If they do, it can be done by:
    // a) disabling minifyEnabled, and applying a proguard or dexguard gradle plugin
    // b) enabling minifyEnabled, and setting android.enableR8 in their gradle properties to false
    // Note: b) only works in projects that use much older versions of AGP as that field is
    // deprecated a while ago (AGP 5.0). We keep it there in case we have customers with old code
    // In general, unless a or b is obviously true, we should assume R8 by default
    fun determineObfuscationType(project: Project, buildType: String): String {
        val isMinifyEnabled = isMinifyEnabled(project, buildType)
        val isProguardGradlePluginApplied = isProguardGradlePluginApplied(project)
        val isDexGuardGradlePluginApplied = isDexGuardGradlePluginApplied(project)

        if (!isMinifyEnabled && !isProguardGradlePluginApplied && !isDexGuardGradlePluginApplied) {
            // no obfuscation in use
            return ObfuscationType.NO_OBFUSCATION.toString()
        } else if (!isMinifyEnabled && isProguardGradlePluginApplied) {
            // case a with proguard plugin
            return ObfuscationType.PROGUARD.toString()
        } else if (!isMinifyEnabled && isDexGuardGradlePluginApplied) {
            // case a with dexguard plugin
            return ObfuscationType.DEXGUARD.toString()
        } else {
            // minifyEnabled, but small chance R8 disabled for proguard via false enableR8 property
            return when {
                !project.hasProperty(ANDROID_ENABLER8) ||
                        project.property(ANDROID_ENABLER8).toString().toBoolean() -> ObfuscationType.R8.toString()
                else -> ObfuscationType.PROGUARD.toString()
            }
        }
    }

    private fun isMinifyEnabled(project: Project, buildType: String): Boolean {
        val androidExtension = project.extensions.findByType(AppExtension::class.java)
        return androidExtension?.buildTypes?.findByName(buildType)?.isMinifyEnabled ?: false
    }

    fun isProguardGradlePluginApplied(project: Project): Boolean {
        return project.plugins.hasPlugin("com.guardsquare.proguard")
    }

    fun isDexGuardGradlePluginApplied(project: Project): Boolean {
        return project.plugins.hasPlugin("dexguard")
    }
}