// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // keep this version in sync with /buildSrc/build.gradle.kts
        classpath(libs.android.plugin)
    }
}

plugins {
    id("splunk.spotless-conventions")
    alias(libs.plugins.publishPlugin)
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
    }
    if (findProperty("release") != "true") {
        version = "$version-SNAPSHOT"
    }
}

subprojects {
    apply(plugin = "splunk.spotless-conventions")
}

nexusPublishing.repositories {
    sonatype {
        username.set(System.getenv("SONATYPE_USER"))
        password.set(System.getenv("SONATYPE_KEY"))
    }
}

