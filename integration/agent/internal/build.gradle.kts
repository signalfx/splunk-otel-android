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
    namespace = "com.cisco.android.rum.integration.agent.internal"
}

dependencies {
    api(project(":integration:agent:module"))

    implementation(project(":common:http"))
    implementation(project(":common:id"))
    implementation(project(":common:utils"))
    implementation(project(":common:storage"))
    implementation(project(":common:otel:internal"))

    implementation(Dependencies.Android.SessionReplay.logger)
}
