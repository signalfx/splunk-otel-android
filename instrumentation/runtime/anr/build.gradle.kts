
import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.instrumentationPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix$instrumentationPrefix${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.cisco.android.rum.anr"
}

dependencies {
    // TODO implementation(project(":common:utils"))
    // TODO implementation(project(":common:logger"))
    implementation(project(":common:otel:api"))

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.jsonassert)
    testImplementation(Dependencies.Test.mockk)
}
