import Dependencies.kotlinStdlibJdk8
import plugins.ConfigAndroidLibrary

plugins {
    id("com.android.application")
    id("kotlin-android")
}

apply<ConfigAndroidLibrary>()

android {
    namespace = "com.smartlook.integrationrun"
    compileSdk = Configurations.Android.appCompileVersion

    defaultConfig {
        applicationId = "com.smartlook.integrationrun"
        minSdk = 21
        targetSdk = Configurations.Android.targetVersion
        versionCode = Configurations.sdkVersionCode
        versionName = Configurations.sdkVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.apply {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = Configurations.Compilation.sourceCompatibility
        targetCompatibility = Configurations.Compilation.targetCompatibility
    }

    kotlinOptions {
        jvmTarget = Configurations.Compilation.jvmTarget
    }

    packagingOptions {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }
}

dependencies {
    implementation(kotlinStdlibJdk8)

    implementation("com.cisco.android:rum-instrumentation-session-recording-core:24.4.10-2246")

    implementation(Dependencies.Android.activityKtx)
    implementation(Dependencies.Android.fragmentKtx)

    /**
     * Explicit version of guava jre must be forced because Roboletric uses one with vulnerabilities.
     */
    testImplementation(Dependencies.guavaJre)
    testImplementation(Dependencies.Test.robolectric)

    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.Test.fragmentTest)
    testImplementation(Dependencies.Android.annotation)

    androidTestImplementation(Dependencies.AndroidTest.junit)
    androidTestImplementation(Dependencies.AndroidTest.testRules)
    androidTestImplementation(Dependencies.AndroidTest.mockk)
    androidTestImplementation(Dependencies.Test.jsonassert)
}
