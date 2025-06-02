import Dependencies.kotlinStdlibJdk8
import plugins.ConfigAndroidApp
import java.net.InetAddress

plugins {
    id("com.android.application")
    id("kotlin-android")
    // Uncomment this to test HttpURLConnection instrumentation
    //id("com.splunk.android.rum-okhttp3-auto-plugin") version "24.4.1"
    //id("com.splunk.android.rum-httpurlconnection-auto-plugin") version "24.4.1"
}

apply<ConfigAndroidApp>()

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
        val realm = project.findProperty("realm") as? String ?: ""
        val token = project.findProperty("rumAccessToken") as? String ?: ""

        buildConfigField("String", "REALM", "\"$realm\"")
        buildConfigField("String", "RUM_ACCESS_TOKEN", "\"$token\"")
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

    implementation(kotlinStdlibJdk8)

    //implementation("com.cisco.android:rum-agent:24.4.10-2246")
    // TODO: this is here just so we do not have duplicate logic, it is not publicly available
    //implementation("com.cisco.android:rum-common-utils:24.4.10-2246")

    implementation(project(":agent"))
    implementation(project(":integration:sessionreplay"))
    implementation(project(":integration:navigation"))

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
