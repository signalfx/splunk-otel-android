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

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(platform(Dependencies.Otel.androidBom))

    implementation(project(":integration:agent:internal"))
    implementation(project(":integration:agent:api"))
    implementation(project(":common:utils"))
    implementation(project(":common:otel"))

    implementation(Dependencies.Otel.androidInstrumentation)

    implementation(Dependencies.Android.fragmentKtx)

    compileOnly(Dependencies.Android.navigationRuntime)

    implementation(Dependencies.Common.utils)
    implementation(Dependencies.Common.logger)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.mockito)
    testImplementation(Dependencies.Test.robolectric)
    testImplementation(Dependencies.Test.androidXTestCore)
    testImplementation(Dependencies.Android.navigationRuntime)

    androidTestImplementation(Dependencies.Test.junit)
    androidTestImplementation(Dependencies.Test.androidXTestCore)
    androidTestImplementation(Dependencies.Test.androidXTestJunit)
    androidTestImplementation(Dependencies.Test.androidXTestRunner)
}
