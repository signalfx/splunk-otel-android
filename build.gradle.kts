buildscript {
    repositories {
        mavenCentral()
        google()

        maven {
            url = uri("$projectDir/instrumentation/buildtime/okhttp3-auto/plugin/repo")
        }

        maven {
            url = uri("$projectDir/instrumentation/buildtime/httpurlconnection-auto/plugin/repo")
        }
    }

    dependencies {
        classpath(Dependencies.gradle)
        classpath(Dependencies.kotlin)
        classpath(Dependencies.http3Auto)
        classpath(Dependencies.httpurlconnectionAuto)
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