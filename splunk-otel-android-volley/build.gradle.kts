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

//val otelVersion = "1.32.1"
//val otelSdkVersion = "1.35.0"
//val otelAlphaVersion = otelVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
//val otelSemconvVersion = "1.23.1-alpha"

dependencies {
    implementation(project(":splunk-otel-android"))
    api(platform(libs.opentelemetry.instrumentation.bom))
    api(platform(libs.opentelemetry.bom))
    compileOnly(libs.opentelemetry.api)
    implementation(libs.opentelemetry.instrumenter.api)
    implementation(libs.opentelemetry.instrumenter.api.semconv)
    compileOnly(libs.android.volley)
    implementation(libs.androidx.core)

//    implementation("androidx.appcompat:appcompat:1.6.1")
//    implementation("androidx.navigation:navigation-fragment:2.7.7")
//    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
//
//
//    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelAlphaVersion"))
//    api(platform("io.opentelemetry:opentelemetry-bom:$otelSdkVersion"))
//
//    api("io.opentelemetry:opentelemetry-api")
//    implementation("io.opentelemetry:opentelemetry-sdk")
//
//
//    implementation("io.opentelemetry.semconv:opentelemetry-semconv:$otelSemconvVersion")
//
//    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
//    testImplementation("androidx.test:core:1.5.0")
    testImplementation(libs.mockwebserver)
    testImplementation(libs.android.volley)
//    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(15))
}

extra["pomName"] = "Splunk Otel Android Volley"
description = "A library for instrumenting Android applications using Volley Library for Splunk RUM"
