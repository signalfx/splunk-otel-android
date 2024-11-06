plugins {
    id("com.android.library")
    id("splunk.android-library-conventions")
    id("splunk.errorprone-conventions")
}

android {
    namespace = "com.splunk.android.rum"

    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        all {
            // provide the version of the library as a resource so it can be used as a span attribute.
            resValue("string", "rum.version", "${project.version}")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(platform(libs.opentelemetry.instrumentation.bom))
    api(platform(libs.opentelemetry.bom))
    api(libs.opentelemetry.android)

    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.instrumentation.okhttp)
    implementation(libs.opentelemetry.exporter.zipkin)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.opentelemetry.exporter.logging)
    implementation(libs.androidx.core)
    implementation(libs.androidx.navigation.fragment)
    api(libs.zipkin.sender.okhttp)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

extra["pomName"] = "Splunk Otel Android"
description = "A library for instrumenting Android applications for Splunk RUM"
