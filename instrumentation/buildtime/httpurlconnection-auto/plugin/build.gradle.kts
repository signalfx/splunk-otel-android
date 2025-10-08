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
            id = "$defaultGroupId.${artifactPrefix}httpurlconnection-auto-plugin"
            implementationClass = "com.splunk.rum.httpurlconnection.auto.plugin.HttpURLInstrumentationPlugin"
            displayName = "Splunk Android HttpURLConnection Auto-Instrumentation Plugin"
        }
    }
}

dependencies {
    implementation(Dependencies.bytebuddyGradlePlugin)
    implementation(Dependencies.bytebuddy)
    implementation(gradleApi())
}

configureGradlePluginSigning()
configureGradlePluginPublishing("${artifactPrefix}httpurlconnection-auto-plugin")
addPublishingOutput()