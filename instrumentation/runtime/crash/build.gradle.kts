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
    namespace = "com.splunk.rum.instrumentation.crash"
}

dependencies {
    implementation(platform(Dependencies.Otel.instrumentationBomAlpha))

    implementation(project(":common:otel"))

    implementation(Dependencies.Otel.api)
    implementation(Dependencies.Otel.sdk)
    implementation(Dependencies.Otel.semConv)
    implementation(Dependencies.Otel.semConvIncubating)

    implementation(Dependencies.Common.logger)
    implementation(Dependencies.Common.utils)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.robolectric)
    testImplementation(Dependencies.Test.androidXTestCore)
    testImplementation(Dependencies.Android.fragmentKtx)
}
