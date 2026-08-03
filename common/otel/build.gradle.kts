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
    namespace = "com.splunk.rum.agent.common.otel"
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

    implementation(Dependencies.Common.logger)
    implementation(Dependencies.Common.job)
    implementation(Dependencies.Common.http)
    implementation(Dependencies.Common.storage)
    implementation(Dependencies.Common.utils)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.mockito)
}
