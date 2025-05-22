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
    namespace = "com.splunk.rum.integration.networkmonitor"
}

dependencies {
    api(platform(Dependencies.Otel.androidBom))

    implementation(project(":integration:agent:internal"))

    implementation(Dependencies.Otel.androidNetworkMonitorInstrumentation)
    implementation(Dependencies.Otel.androidServices)
    implementation(Dependencies.Otel.androidCommon)
    implementation(Dependencies.Otel.semConvIncubating)

    implementation(Dependencies.SessionReplay.commonLogger)

}
