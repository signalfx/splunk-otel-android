package plugins

import Configurations
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType

class ConfigAndroidLibrary : Plugin<Project> by local plugin {
    apply<ConfigLint>()
    apply<ConfigJacoco>()

    tasks.withType<Test> {
        systemProperty(
            "robolectric.dependency.repo.url",
            "https://maven-central.storage-download.googleapis.com/maven2"
        )
    }

    android {
        buildFeatures {
            buildConfig = true
        }

        compileSdk = Configurations.Android.compileVersion

        defaultConfig {
            minSdk = Configurations.Android.minVersion
            targetSdk = Configurations.Android.targetVersion

            consumerProguardFiles("consumer-rules.pro")

            buildConfigField("String", "VERSION_NAME", "\"${Configurations.sdkVersionName}\"")
            buildConfigField("String", "VERSION_CODE", "\"${Configurations.sdkVersionCode}\"")
        }

        compileOptions {
            sourceCompatibility = Configurations.Compilation.sourceCompatibility
            targetCompatibility = Configurations.Compilation.targetCompatibility
        }

        kotlinOptions {
            jvmTarget = Configurations.Compilation.jvmTarget
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }

        testOptions {
            unitTests.isIncludeAndroidResources = true
        }

        buildTypes {
            debug {
                enableAndroidTestCoverage = true
                enableUnitTestCoverage = true
            }
        }
    }
}