package plugins

import Dependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class ConfigAndroidLibrary : Plugin<Project> by local plugin {
    apply<ConfigLint>()
    apply<ConfigJacoco>()

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