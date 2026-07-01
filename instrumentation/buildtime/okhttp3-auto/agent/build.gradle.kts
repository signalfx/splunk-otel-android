import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "${artifactPrefix}okhttp3-auto-agent")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.instrumentation.okhttp3.agent"
}

dependencies {
    implementation(platform(Dependencies.Otel.instrumentationBomAlpha))
    implementation(Dependencies.bytebuddy)
    implementation(Dependencies.okhttp)
    /**
     * Okio must be explicitly included since a newer version is being enforced than what is transitively used by OkHttp.
     */
    implementation(Dependencies.okio)
    implementation(Dependencies.Otel.api)
    implementation(project(":instrumentation:runtime:okhttp3-auto"))
}
