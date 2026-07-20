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
    namespace = "com.splunk.rum.instrumentation.anr"
}

dependencies {
    implementation(platform(Dependencies.Otel.androidBom))
    implementation(platform(Dependencies.Otel.instrumentationBomAlpha))

    implementation(Dependencies.Otel.api)
    implementation(Dependencies.Otel.semConv)

    implementation(Dependencies.SessionReplay.commonLogger)
    implementation(Dependencies.SessionReplay.commonUtils)

    testImplementation(platform(Dependencies.Otel.androidBom))
    testImplementation(Dependencies.Otel.sdk)
    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.robolectric)
    testImplementation(Dependencies.Test.androidXTestCore)
    testImplementation(Dependencies.Test.mockito)
}
