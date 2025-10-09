import utils.artifactPrefix
import utils.defaultGroupId

private val pluginName = "mapping-file-plugin"

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
            id = "$defaultGroupId.${artifactPrefix}$pluginName"
            implementationClass = "com.splunk.rum.mappingfile.plugin.MappingFilePlugin"
            displayName = "Splunk Android Mapping File Plugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(Dependencies.gradle)
}

configureGradlePluginSigning()
configureGradlePluginPublishing("${artifactPrefix}$pluginName")
addPublishingOutput()