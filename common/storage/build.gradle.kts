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
    set(artifactIdProperty, "$artifactPrefix$commonPrefix${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.common.storage"
}

dependencies {
    implementation(Dependencies.Common.logger)
    implementation(Dependencies.Common.storage)
    implementation(Dependencies.Common.utils)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.androidXTestCore)
    testImplementation(Dependencies.Test.androidXTestJunit)
    testImplementation(Dependencies.Test.robolectric)
}
