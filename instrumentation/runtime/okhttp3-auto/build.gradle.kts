import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.instrumentationPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix$instrumentationPrefix${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.instrumentation.okhttp3.auto"
}

dependencies {
    implementation(platform(Dependencies.Otel.instrumentationBomAlpha))

    implementation(project(":instrumentation:runtime:okhttp3-common"))
    implementation(project(":common:utils"))

    implementation(Dependencies.okhttp)
    /**
     * Okio must be explicitly included since a newer version is being enforced than what is transitively used by OkHttp.
     */
    implementation(Dependencies.okio)

    implementation(Dependencies.Otel.instrumentationApi)
    implementation(Dependencies.Otel.instrumentationApiIncubator)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Otel.sdk)
}
