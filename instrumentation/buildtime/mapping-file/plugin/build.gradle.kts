import plugins.ConfigPlugin
import plugins.pluginIdSuffixProperty
import plugins.pluginImplementationClassProperty
import plugins.pluginDisplayNameProperty
import plugins.pluginArtifactIdProperty
import utils.artifactPrefix

plugins {
    kotlin("jvm")
}

apply<ConfigPlugin>()

ext {
    set(pluginIdSuffixProperty, "${artifactPrefix}mapping-file-plugin")
    set(pluginImplementationClassProperty, "com.splunk.rum.mappingfile.plugin.MappingFilePlugin")
    set(pluginDisplayNameProperty, "Splunk Android Mapping File Plugin")
    set(pluginArtifactIdProperty, "${artifactPrefix}mapping-file-plugin")
}

dependencies {
    implementation(gradleApi())
    implementation(Dependencies.gradle)
}