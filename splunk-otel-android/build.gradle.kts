plugins {
    id("com.android.library")
    id("splunk.android-library-conventions")
    id("splunk.errorprone-conventions")
}

android {
    namespace = "com.splunk.android.rum"

    compileSdk = 34
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

val otelVersion = "1.30.0-SNAPSHOT"
val otelAlphaVersion = otelVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")
val otelSemconvVersion = "1.21.0-alpha"

dependencies {
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelAlphaVersion"))
    implementation("io.opentelemetry.android:instrumentation:0.3.0-alpha-SNAPSHOT")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.10"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.navigation:navigation-fragment:2.7.4")

    implementation("io.opentelemetry:opentelemetry-sdk")
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin")
    implementation("io.zipkin.reporter2:zipkin-sender-okhttp3")
    implementation("io.opentelemetry:opentelemetry-exporter-logging")

    implementation("io.opentelemetry.semconv:opentelemetry-semconv:$otelSemconvVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0")

    api("io.opentelemetry:opentelemetry-api")
    api("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.6.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.vintage:junit-vintage-engine")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.assertj:assertj-core:3.24.2")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

extra["pomName"] = "Splunk Otel Android"
description = "A library for instrumenting Android applications for Splunk RUM"
