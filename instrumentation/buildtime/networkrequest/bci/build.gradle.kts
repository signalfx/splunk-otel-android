import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.networkRequestPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
}

apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix${networkRequestPrefix}${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.cisco.android.rum.networkrequest.bci"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = Configurations.Compilation.sourceCompatibility
        targetCompatibility = Configurations.Compilation.targetCompatibility
    }
}

dependencies {
    coreLibraryDesugaring(Dependencies.desugar)
    implementation(project(":instrumentation:runtime:networkrequest:library"))
    implementation(Dependencies.bytebuddy)

    /**
     * Okio must be explicitly included since a newer version is being enforced than what is transitively used by OkHttp.
     */
    implementation(Dependencies.okhttp)
    implementation(Dependencies.okio)
}
