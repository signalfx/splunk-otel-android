import plugins.ConfigAndroidApp
import java.net.InetAddress

plugins {
    id("com.android.application")
    id("kotlin-android")
    // Uncomment this to test HttpURLConnection instrumentation
    //id("com.splunk.rum-okhttp3-auto-plugin") version "2.0.0-alpha.1-SNAPSHOT"
    //id("com.splunk.rum-httpurlconnection-auto-plugin") version "2.0.0-alpha.1-SNAPSHOT"
    // Uncomment this to test mapping file plugin
//    id("com.splunk.rum-mapping-file-plugin") version "2.0.0-alpha.1-SNAPSHOT"
}

apply<ConfigAndroidApp>()

// Uncomment this to test the mapping file plugin with custom configuration
//splunkRum {
//    enabled = true
//    failBuildOnUploadFailure = false
//    apiAccessToken = "your-api-access-token"
//    realm = "us0"
//}

android {
    namespace = "com.splunk.app"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.splunk.app"
        versionCode = Configurations.sdkVersionCode
        versionName = Configurations.sdkVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.put("clearPackageData", "true")

        // Read from global gradle.properties (~/.gradle/gradle.properties)
        // If not found, fallback to empty string
        val realm = project.findProperty("splunkRealm") as? String ?: ""
        val token = project.findProperty("splunkRumAccessToken") as? String ?: ""

        buildConfigField("String", "SPLUNK_REALM", "\"$realm\"")
        buildConfigField("String", "SPLUNK_RUM_ACCESS_TOKEN", "\"$token\"")
    }

    buildTypes {
        getByName("debug") {
            resValue("bool", "leak_canary_add_launcher_icon", "false")
            val ip = InetAddress.getLocalHost().hostAddress
            buildConfigField("String", "IP_ADDRESS", "\"$ip\"")
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildFeatures {
        viewBinding = true
    }

    packagingOptions {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    api(platform(Dependencies.Otel.instrumentationBomAlpha))

    implementation(project(":agent"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")

    implementation(Dependencies.SessionReplay.commonLogger)
    implementation(Dependencies.SessionReplay.commonUtils)

    implementation(Dependencies.Android.appcompat)
    implementation(Dependencies.Android.constraintLayout)
    implementation(Dependencies.Android.activityKtx)
    implementation(Dependencies.Android.fragmentKtx)
    implementation(Dependencies.Android.material)

    /**
     * Okio must be explicitly included since a newer version is being enforced than what is transitively used by OkHttp.
     */
    implementation(Dependencies.okhttp)
    implementation(Dependencies.okio)

    debugImplementation(Dependencies.AndroidDebug.leakCanary)

    /**
     * Explicit version of guava jre must be forced because ext truth uses one with vulnerabilities.
     */
    androidTestImplementation(Dependencies.guavaAndroid)
    androidTestImplementation(Dependencies.AndroidTest.testExtTruth)

    androidTestImplementation(Dependencies.AndroidTest.junit)
    androidTestImplementation(Dependencies.AndroidTest.mockk)
    androidTestImplementation(Dependencies.AndroidTest.serialization)
    androidTestImplementation(Dependencies.AndroidTest.testRules)
    androidTestImplementation(Dependencies.AndroidTest.testRunner)
    androidTestImplementation(Dependencies.AndroidTest.uiAutomator)

    /**
     * Jsoup must be explicitly included since a newer version is being enforced than what is transitively used by espresso contrib.
     */
    androidTestImplementation(Dependencies.AndroidTest.Espresso.contrib)
    androidTestImplementation(Dependencies.AndroidTest.Espresso.jsoup)

    androidTestImplementation(Dependencies.AndroidTest.Espresso.core)
    androidTestImplementation(Dependencies.AndroidTest.Espresso.idlingConcurrent)
    androidTestImplementation(Dependencies.AndroidTest.Espresso.idlingResource)
    androidTestImplementation(Dependencies.AndroidTest.Espresso.intents)
    androidTestImplementation(Dependencies.AndroidTest.Espresso.web)

    androidTestImplementation(Dependencies.okhttp)
    androidTestImplementation(Dependencies.okio)
    androidTestImplementation(Dependencies.AndroidTest.okhttpLogging)
    
    androidTestImplementation(Dependencies.Test.jsonassert)

    androidTestUtil(Dependencies.AndroidTest.testOrchestrator)

    /**
     * Explicit version of guava jre must be forced because ext truth uses one with vulnerabilities.
     */
    implementation(Dependencies.guavaAndroid)
}

tasks.register<Exec>("startOtelCollectorForTests") {
    group = "docker"
    description = "Start services defined in docker-compose.yaml"
    commandLine("docker-compose", "-f", "docker-compose.yaml", "up", "-d")
}
