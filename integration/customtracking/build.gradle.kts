import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.integrationPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix$integrationPrefix${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.integration.customtracking"
}

dependencies {
    api(platform(Dependencies.Otel.instrumentationBomAlpha))
    api(Dependencies.Otel.api)
    implementation(project(":integration:agent:api"))
    implementation(project(":common:otel"))
    implementation(Dependencies.SessionReplay.commonLogger)
}