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
    set(pluginIdSuffixProperty, "${artifactPrefix}okhttp3-auto-plugin")
    set(pluginImplementationClassProperty, "com.splunk.rum.okhttp3.auto.plugin.OkHttp3InstrumentationPlugin")
    set(pluginDisplayNameProperty, "Splunk Android OkHttp3 Auto-Instrumentation Plugin")
    set(pluginArtifactIdProperty, "${artifactPrefix}okhttp3-auto-plugin")
}

dependencies {
    implementation(Dependencies.bytebuddyGradlePlugin)
    implementation(Dependencies.bytebuddy)
    implementation(gradleApi())
}