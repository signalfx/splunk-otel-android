import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
}

val localProperties = Properties()
localProperties.load(FileInputStream(rootProject.file("local.properties")))

android {
    namespace = "com.splunk.android.sample"

    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.splunk.android.sample"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    buildTypes {
        all {
            val realm = localProperties["rum.realm"] as String?
            val accessToken = localProperties["rum.access.token"] as String?
            resValue("string", "rum_realm", realm ?: "us0")
            resValue("string", "rum_access_token", accessToken ?: "dummyAuth")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        release {
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
}

val otelVersion = "1.30.0-SNAPSHOT"
val otelAlphaVersion = otelVersion.replaceFirst("(-SNAPSHOT)?$".toRegex(), "-alpha$1")

dependencies {
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelAlphaVersion"))

    implementation("io.opentelemetry.android:instrumentation:0.2.0-alpha")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.webkit:webkit:1.8.0")
    implementation("androidx.browser:browser:1.6.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.7.4")
    implementation("androidx.navigation:navigation-ui:2.7.4")
    implementation(project(":splunk-otel-android"))
    implementation(project(":splunk-otel-android-volley"))
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.work:work-runtime:2.8.1")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv")

    implementation("io.opentelemetry:opentelemetry-api-events")
    implementation("io.opentelemetry:opentelemetry-sdk-logs")
    implementation("io.opentelemetry:opentelemetry-sdk")

    testImplementation("junit:junit:4.13.2")
}
