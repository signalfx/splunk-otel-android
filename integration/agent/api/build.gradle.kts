import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.integrationPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
    //id("org.jetbrains.dokka")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix${integrationPrefix}agent-${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.integration.agent.api"
}

dependencies {
    api(platform(Dependencies.Otel.instrumentationBomAlpha))
    api(platform(Dependencies.Otel.androidBom))

    implementation(Dependencies.Otel.androidCore)

    api(project(":common:otel"))
    api(project(":integration:agent:common"))

    implementation(project(":integration:agent:internal"))
    implementation(project(":common:storage"))
    implementation(project(":common:utils"))

    implementation(Dependencies.Otel.semConv)

    implementation(Dependencies.SessionReplay.commonLogger)
    implementation(Dependencies.SessionReplay.commonStorage)
    implementation(Dependencies.SessionReplay.commonUtils)

    compileOnly(Dependencies.Android.Compose.ui)
}
