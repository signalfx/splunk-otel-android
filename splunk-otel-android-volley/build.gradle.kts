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

val otelVersion = "1.32.1"
val otelSdkVersion = "1.35.0"
val otelAlphaVersion = otelVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
val otelSemconvVersion = "1.23.1-alpha"

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    compileOnly("com.android.volley:volley:1.2.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(project(":splunk-otel-android"))

    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelAlphaVersion"))
    api(platform("io.opentelemetry:opentelemetry-bom:$otelSdkVersion"))

    api("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-sdk")

    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")

    implementation("io.opentelemetry.semconv:opentelemetry-semconv:$otelSemconvVersion")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("org.robolectric:robolectric:4.12.1")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("com.google.mockwebserver:mockwebserver:20130706")
    testImplementation("com.android.volley:volley:1.2.1")
    testImplementation("org.apache.httpcomponents:httpclient:4.5.14")
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(15))
}

extra["pomName"] = "Splunk Otel Android Volley"
description = "A library for instrumenting Android applications using Volley Library for Splunk RUM"
