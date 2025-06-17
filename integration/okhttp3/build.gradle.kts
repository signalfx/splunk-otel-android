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
    namespace = "com.splunk.rum.integration.okhttp"
}

dependencies {
    implementation(project(":integration:agent:api"))

    implementation(Dependencies.SessionReplay.commonUtils)

    implementation(Dependencies.okhttp)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.jsonassert)
    testImplementation(Dependencies.Test.mockWebServer)
}
