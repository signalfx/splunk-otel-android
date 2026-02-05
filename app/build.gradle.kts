import plugins.ConfigAndroidApp
import java.net.InetAddress

plugins {
    id("com.android.application")
    id("kotlin-android")
    // Uncomment this to test HttpURLConnection instrumentation
    //id("com.splunk.rum-okhttp3-auto-plugin") version "2.1.4-SNAPSHOT"
    //id("com.splunk.rum-httpurlconnection-auto-plugin") version "2.1.4-SNAPSHOT"
    // Uncomment this to test mapping file plugin
    //id("com.splunk.rum-mapping-file-plugin") version "2.1.1-SNAPSHOT"
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

    implementation(project(":agent"))

    implementation(AppDependencies.kotlinStdlib)

    implementation(AppDependencies.Android.appcompat)
    implementation(AppDependencies.Android.constraintLayout)
    implementation(AppDependencies.Android.activityKtx)
    implementation(AppDependencies.Android.fragmentKtx)
    implementation(AppDependencies.Android.material)

    /**
     * Okio must be explicitly included since a newer version is being enforced than what is transitively used by OkHttp.
     */
    implementation(AppDependencies.okhttp)
    implementation(AppDependencies.okio)

    debugImplementation(AppDependencies.leakCanary)
}

tasks.register<Exec>("startOtelCollectorForTests") {
    group = "docker"
    description = "Start services defined in docker-compose.yaml"
    commandLine("docker-compose", "-f", "docker-compose.yaml", "up", "-d")
}
