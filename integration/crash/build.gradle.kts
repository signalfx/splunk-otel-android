import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.integrationPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix$integrationPrefix${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.integration.crash"
}

dependencies {
    implementation(platform(Dependencies.Otel.androidBom))
    implementation(platform(Dependencies.Otel.instrumentationBomAlpha))
    
    implementation(project(":integration:agent:internal"))
    implementation(project(":common:otel"))

    implementation(Dependencies.Otel.androidCrashInstrumentation)

    implementation(Dependencies.Otel.instrumentationApi)

    implementation(Dependencies.SessionReplay.commonLogger)
    implementation(Dependencies.SessionReplay.commonUtils)
}