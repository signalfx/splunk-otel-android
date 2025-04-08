import java.io.FileInputStream
import java.util.Properties
import java.util.UUID

plugins {
    id("com.android.application")
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.compose)
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
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
        compose = true
    }

    // Different product flavors for different variants
    flavorDimensions += "version"

    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
        }

        create("pro") {
            dimension = "version"
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
        }
    }

    buildTypes {
        all {
            val realm = localProperties["rum.realm"] as String?
            val accessToken = localProperties["rum.access.token"] as String?
            resValue("string", "rum_realm", realm ?: "us0")
            resValue("string", "rum_access_token", accessToken ?: "dummyAuth")
            isMinifyEnabled = false // set to true to enable obfuscation
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.configureEach {
        val uniqueBuildId = UUID.randomUUID().toString()

        // Will only work if customer has added the placeholder metadata block
        this.mergedFlavor.manifestPlaceholders["splunkBuildId"] = uniqueBuildId

        logger.lifecycle("Splunk: Variant $name assigned build ID: $uniqueBuildId")

        // Create task to save build ID to a reference file and processed manifest
        val variantName = name
        val capitalizedVariantName = variantName.replaceFirstChar { it.uppercase() }

        // Forces the manifest processing task to always run, even during incremental builds
        tasks.named("process${capitalizedVariantName}Manifest").configure {
            outputs.upToDateWhen { false }
        }

        tasks.register("recordSplunkBuildId$capitalizedVariantName") {
            description = "Records the Splunk build ID for the $variantName variant"
            group = "Splunk"
            outputs.upToDateWhen { false } // Ensures that this task always runs, even on incremental builds

            doLast {
                try {
                    val buildDirectory = layout.buildDirectory.get().asFile
                    val buildIdFile = buildDirectory.resolve("splunk/build_ids.txt")

                    buildIdFile.parentFile.mkdirs()

                    // Add app id and version code to the top of the file if it doesn't exist yet
                    if (!buildIdFile.exists()) {
                        val appId = android.defaultConfig.applicationId ?: "unknown"
                        val versionCode = android.defaultConfig.versionCode ?: 0

                        buildIdFile.writeText("""
                            # App ID: $appId
                            # Version Code: $versionCode
                    
                            """.trimIndent())
                        logger.lifecycle("Created build ID file with app information")
                    }

                    val mergedManifestPath = "${buildDirectory}/intermediates/merged_manifests/${variantName}/AndroidManifest.xml"
                    // Appending build ID of this variant to file
                    buildIdFile.appendText("Variant: $variantName, Build ID: $uniqueBuildId, Manifest: $mergedManifestPath\n")
                    logger.lifecycle("Recorded build ID for $variantName in ${buildIdFile.absolutePath}")
                } catch (e: Exception) {
                    logger.error("Failed to record build ID for $variantName: ${e.message}")
                }
            }
        }

        // Only run after assembly of this variant is done
        tasks.named("assemble$capitalizedVariantName").configure {
            finalizedBy("recordSplunkBuildId$capitalizedVariantName")
        }
    }
}

composeCompiler {
    enableStrongSkippingMode = true
}

dependencies {
    api(platform(libs.opentelemetry.instrumentation.bom))

    implementation(project(":splunk-otel-android"))
    implementation(project(":splunk-otel-android-volley"))
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    coreLibraryDesugaring(libs.desugarJdkLibs)

    implementation(libs.androidx.webkit)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.android.volley)
    implementation(libs.androidx.work)
    implementation(libs.opentelemetry.sdk)
    implementation(libs.opentelemetry.api.incubator)
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
