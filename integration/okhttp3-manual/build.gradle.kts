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
    namespace = "com.splunk.rum.integration.okhttp3.manual"
}

dependencies {
    api(platform(Dependencies.Otel.androidBom))

    implementation(project(":integration:agent:api"))
    implementation(project(":integration:agent:internal"))
    implementation(project(":integration:okhttp:common"))

    implementation(Dependencies.Otel.androidInstrumentation)
    implementation(Dependencies.Otel.instrumentationOkHttp3Library)

    implementation(Dependencies.okhttp)

    implementation(Dependencies.SessionReplay.commonLogger)
}
