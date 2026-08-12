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
    namespace = "com.splunk.rum.integration.agent.internal"
}

dependencies {
    api(project(":integration:agent:common"))
    implementation(project(":common:otel"))
    implementation(project(":common:storage"))
    implementation(project(":common:utils"))

    implementation(Dependencies.Common.logger)
    implementation(Dependencies.Common.http)
    implementation(Dependencies.Common.storage)
    implementation(Dependencies.Common.utils)

    compileOnly(Dependencies.Android.Compose.ui)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.mockito)
    testImplementation(Dependencies.Test.robolectric)
}
