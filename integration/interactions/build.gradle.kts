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
    namespace = "com.splunk.rum.integration.interactions"
}

dependencies {
    implementation(project(":common:otel"))
    implementation(project(":integration:agent:internal"))
    api(project(":integration:sessionreplay"))

    implementation(Dependencies.Common.logger)
    implementation(Dependencies.Common.utils)

    implementation(Dependencies.SessionReplay.instrumentationSessionRecordingFrameCapturer)
    implementation(Dependencies.SessionReplay.instrumentationSessionRecordingInteractions)

    compileOnly(Dependencies.Android.Compose.ui)

    implementation(platform(Dependencies.Otel.androidBom))
    implementation(Dependencies.Otel.androidInstrumentation)

    testImplementation(Dependencies.Test.junit)
}
