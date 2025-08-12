import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.commonPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix${commonPrefix}${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.common.otel"
}

dependencies {
    api(platform(Dependencies.Otel.instrumentationBomAlpha))
    compileOnly(Dependencies.Android.annotation)

    implementation(project(":common:storage"))

    api(Dependencies.Otel.sdk)
    api(Dependencies.Otel.exporterOtlpCommon)
    api(Dependencies.Otel.exporterOtlp) {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
    api(Dependencies.Otel.semConv)
    api(Dependencies.Otel.semConvIncubating)

    implementation(Dependencies.SessionReplay.commonLogger)
    implementation(Dependencies.SessionReplay.commonJob)
    implementation(Dependencies.SessionReplay.commonHttp)
    implementation(Dependencies.SessionReplay.commonStorage)
    implementation(Dependencies.SessionReplay.commonUtils)
}
