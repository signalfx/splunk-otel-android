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
    namespace = "com.splunk.rum.instrumentation.httpurlconnection.auto"
}

dependencies {
    implementation(platform(Dependencies.Otel.instrumentationBomAlpha))

    implementation(Dependencies.Otel.instrumentationApi)
    implementation(Dependencies.Otel.instrumentationApiIncubator)
}
