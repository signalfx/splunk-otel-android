buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath(Dependencies.gradle)
        classpath(Dependencies.kotlin)
    }
}

allprojects {
    apply<plugins.ConfigKtLint>()

    // Enforce lower versions of certain libraries to avoid compatibility issues
    // introduced by higher versions from OpenTelemetry dependencies.
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
            force("androidx.core:core:1.13.1")
            force("androidx.core:core-ktx:1.13.1")
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        google()

        maven {
            setUrl("https://sdk.smartlook.com/android/release")
        }
    }
}

tasks.register("buildAARs") {
    description = "Create AAR for all modules"
    group = "build"

    subprojects.forEach { module ->
        if (module.plugins.hasPlugin("com.android.library"))
            dependsOn("${module.path}:assembleRelease")
    }
}