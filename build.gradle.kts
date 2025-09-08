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
            force("androidx.core:core-ktx:1.17.0")
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    afterEvaluate {
        if (!isReleaseBuild()) {
            version = "$version-SNAPSHOT"
        }
    }
}

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

fun Project.isReleaseBuild(): Boolean {
    return findProperty("release") == "true"
}
