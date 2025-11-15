package plugins

import AppDependencies
import Configurations
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class ConfigAndroidApp : Plugin<Project> by local plugin {
    apply<ConfigLint>()

    androidApplication {
        compileSdk = Configurations.Android.appCompileVersion

        defaultConfig {
            minSdk = Configurations.Android.minVersion
            targetSdk = Configurations.Android.targetVersion
        }

        compileOptions {
            isCoreLibraryDesugaringEnabled = true

            sourceCompatibility = Configurations.Compilation.sourceCompatibility
            targetCompatibility = Configurations.Compilation.targetCompatibility
        }

        kotlinOptions {
            jvmTarget = Configurations.Compilation.jvmTarget
            freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        }
    }

    dependencies {
        "coreLibraryDesugaring"(AppDependencies.desugar)
    }
}