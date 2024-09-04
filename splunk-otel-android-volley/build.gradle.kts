import java.time.Duration

plugins {
    id("com.android.library")
    id("splunk.android-library-conventions")
    id("splunk.errorprone-conventions")
}

android {
    namespace = "com.splunk.android.rum.volley"

    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
        isCoreLibraryDesugaringEnabled = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true

        unitTests.all {
            it.testLogging.showStandardStreams = true
            it.testLogging {
                events("started", "passed", "failed")
            }
        }
    }
}

dependencies {
    implementation(project(":splunk-otel-android"))
    api(platform(libs.opentelemetry.instrumentation.bom))
    api(platform(libs.opentelemetry.bom))
    compileOnly(libs.opentelemetry.api)
    implementation(libs.opentelemetry.instrumenter.api)
    implementation(libs.opentelemetry.instrumenter.api.incubator)
    implementation(libs.opentelemetry.semconv.incubating)
    compileOnly(libs.android.volley)
    implementation(libs.androidx.core)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.android.volley)
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(15))
}

extra["pomName"] = "Splunk Otel Android Volley"
description = "A library for instrumenting Android applications using Volley Library for Splunk RUM"
