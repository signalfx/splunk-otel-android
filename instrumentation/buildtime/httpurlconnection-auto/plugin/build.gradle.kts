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
    set(pluginIdSuffixProperty, "${artifactPrefix}httpurlconnection-auto-plugin")
    set(pluginImplementationClassProperty, "com.splunk.rum.httpurlconnection.auto.plugin.HttpURLInstrumentationPlugin")
    set(pluginDisplayNameProperty, "Splunk Android HttpURLConnection Auto-Instrumentation Plugin")
    set(pluginArtifactIdProperty, "${artifactPrefix}httpurlconnection-auto-plugin")
}

dependencies {
    implementation(Dependencies.bytebuddyGradlePlugin)
    implementation(Dependencies.bytebuddy)
    implementation(gradleApi())
}