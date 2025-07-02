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

plugins {
    id(Dependencies.nexusPublishPluginId) version Dependencies.nexusPublishPluginVersion
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

//    if (findProperty("release") != "true") {
//        version = "$version-SNAPSHOT"
//    }

    val releaseProperty = findProperty("release")
    println("DEBUGGING: release property = '$releaseProperty'")
    println("DEBUGGING: original version = '$version'")

    if (findProperty("release") != "true") {
        version = "$version-SNAPSHOT"
        println("DEBUGGING: updated version to '$version'")
    } else {
        println("DEBUGGING: keeping original version (release mode)")
    }
}

// Configure nexus publishing for Maven Central identical to main branch
nexusPublishing {
    packageGroup.set("com.splunk")
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_KEY"))
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}