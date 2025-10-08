import utils.artifactPrefix
import utils.defaultGroupId

plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    id("maven-publish")
    id("signing")
}

group = defaultGroupId
version = Configurations.sdkVersionName

createStandardJars()

gradlePlugin {
    plugins {
        create("androidInstrumentationPlugin") {
            id = "$defaultGroupId.${artifactPrefix}okhttp3-auto-plugin"
            implementationClass = "com.splunk.rum.okhttp3.auto.plugin.OkHttp3InstrumentationPlugin"
            displayName = "Splunk Android OkHttp3 Auto-Instrumentation Plugin"
        }
    }
}

dependencies {
    implementation(Dependencies.bytebuddyGradlePlugin)
    implementation(Dependencies.bytebuddy)
    implementation(gradleApi())
}

configureGradlePluginSigning()
configureGradlePluginPublishing("${artifactPrefix}okhttp3-auto-plugin")
addPublishingOutput()