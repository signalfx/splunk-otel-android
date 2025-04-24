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
    set(artifactIdProperty, "$artifactPrefix${integrationPrefix}agent-${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.integration.agent.attributes"
}

dependencies {
    api(platform(Dependencies.Otel.instrumentationBomAlpha))
    api(Dependencies.Otel.api)
}
