import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "${artifactPrefix}httpurlconnection-auto-agent")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.instrumentation.httpurlconnection.agent"
}

dependencies {
    implementation(Dependencies.bytebuddy)
    implementation(project(":instrumentation:runtime:httpurlconnection-auto"))
}
