import plugins.ConfigAndroidLibrary
import utils.artifactIdProperty
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
}

apply<ConfigAndroidLibrary>()
apply<plugins.ConfigPublish>()

ext {
    set(artifactIdProperty, "rum-agent")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.cisco.android.rum"
}

dependencies {
    api(project(":integration:agent:api"))
    // TODO api(project(":integration:session-recording"))
    api(project(":integration:crash"))
    api(project(":integration:anr"))
    api(project(":integration:networkrequest"))
    api(project(":integration:startup"))
    api(project(":integration:interactions"))
}

