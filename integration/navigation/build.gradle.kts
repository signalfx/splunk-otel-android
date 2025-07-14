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
    namespace = "com.splunk.rum.integration.navigation"
}

dependencies {
    implementation(project(":integration:agent:internal"))
    implementation(project(":integration:agent:api"))
    implementation(project(":common:utils"))
    implementation(project(":common:otel"))

    implementation(Dependencies.Android.fragmentKtx)
    implementation(Dependencies.Otel.androidInstrumentation)
    implementation(Dependencies.SessionReplay.commonUtils)
    implementation(Dependencies.SessionReplay.commonLogger)
}
