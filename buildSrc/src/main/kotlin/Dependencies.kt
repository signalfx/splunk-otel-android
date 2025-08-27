object Dependencies {

    // Project level dependencies

    private const val gradleVersion = "8.6.0"
    private const val kotlinVersion = "1.8.0"
    private const val ktlintVersion = "1.5.0"
    private const val desugarVersion = "2.1.3"
    private const val bytebuddyVersion = "1.17.2"

    const val gradle = "com.android.tools.build:gradle:$gradleVersion"
    const val gradleApi = "com.android.tools.build:gradle-api:$gradleVersion"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val ktlint = "com.pinterest.ktlint:ktlint-cli:$ktlintVersion"
    const val desugar = "com.android.tools:desugar_jdk_libs:$desugarVersion"

    // Nexus publish plugin
    const val nexusPublishPluginId = "io.github.gradle-nexus.publish-plugin"
    const val nexusPublishPluginVersion = "2.0.0"

    // SDK module level dependencies

    /**
     * OkHttp 4.11.0 is the latest version that does not mandate the use of Kotlin 1.9.0.
     * Since this version includes Okio transitively with vulnerabilities, we explicitly set the Okio version to a safer one.
     */
    private const val okhttpVersion = "4.11.0"
    private const val okioVersion = "3.4.0"

    const val kotlinStdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
    const val okhttp = "com.squareup.okhttp3:okhttp:$okhttpVersion"
    const val okio = "com.squareup.okio:okio:$okioVersion"
    const val bytebuddy = "net.bytebuddy:byte-buddy:$bytebuddyVersion"
    const val bytebuddyGradlePlugin = "net.bytebuddy:byte-buddy-gradle-plugin:$bytebuddyVersion"

    /**
     * The bellow listed dependencies transitively include problematic guava version and cannot be updated.
     * We will force the usage of version 32.0.1, which is the closest version to the one currently used without vulnerabilities.
     *
     * guava:31.1-jre is used by:
     * - org.robolectric:robolectric:4.12.1
     *
     * guava:31.1-android is used by:
     * - androidx.test.ext:truth:1.6.0-alpha03
     * - com.google.android.exoplayer:exoplayer:2.19.1
     */
    private const val guavaJreVersion = "32.0.1"
    const val guavaAndroid = "com.google.guava:guava:$guavaJreVersion-android"

    object Test {
        private const val junitVersion = "4.13.2"
        private const val jsonassertVersion = "1.5.3"
        private const val mockkVersion = "1.12.4"
        private const val mockWebServerVersion = "4.10.0"

        const val junit = "junit:junit:$junitVersion"
        const val jsonassert = "org.skyscreamer:jsonassert:$jsonassertVersion"
        const val mockk = "io.mockk:mockk:$mockkVersion"
        const val mockWebServer = "com.squareup.okhttp3:mockwebserver:$mockWebServerVersion"
    }

    object Android {

        private const val annotationVersion = "1.6.0"
        private const val appcompatVersion = "1.6.1"
        private const val materialVersion = "1.9.0"

        const val annotation = "androidx.annotation:annotation:$annotationVersion"
        const val appcompat = "androidx.appcompat:appcompat:$appcompatVersion"
        const val material = "com.google.android.material:material:$materialVersion"

        // Test application

        private const val constraintLayoutVersion = "2.1.4"
        private const val activityKtxVersion = "1.2.2"
        private const val fragmentKtxVersion = "1.3.3"

        const val constraintLayout = "androidx.constraintlayout:constraintlayout:$constraintLayoutVersion"
        const val activityKtx = "androidx.activity:activity-ktx:$activityKtxVersion"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentKtxVersion"

        object Compose {
            private const val version = "1.2.1" // No need to update

            const val ui = "androidx.compose.ui:ui:$version"
        }
    }

    object SessionReplay {
        private const val version = "1.0.21"

        const val bridge = "com.splunk.android:sr-bridge:$version"
        const val commonEncoder = "com.splunk.android:sr-common-encoder:$version"
        const val commonHttp = "com.splunk.android:sr-common-http:$version"
        const val commonId = "com.splunk.android:sr-common-id:$version"
        const val commonJob = "com.splunk.android:sr-common-job:$version"
        const val commonStorage = "com.splunk.android:sr-common-storage:$version"
        const val commonUtils = "com.splunk.android:sr-common-utils:$version"
        const val commonLogger = "com.splunk.android:sr-common-logger:$version"

        const val instrumentationSessionRecordingCore = "com.splunk.android:sr-instrumentation-session-recording-core:$version"
        const val instrumentationSessionRecordingFrameCapturer = "com.splunk.android:sr-instrumentation-session-recording-frame-capturer:$version"
        const val instrumentationSessionRecordingInteractions = "com.splunk.android:sr-instrumentation-session-recording-interactions:$version"
        const val instrumentationSessionRecordingScreenshot = "com.splunk.android:sr-instrumentation-session-recording-screenshot:$version"
        const val instrumentationSessionRecordingWireframe = "com.splunk.android:sr-instrumentation-session-recording-wireframe:$version"

        const val debugger = "com.splunk.android:sr-debugger:$version"
    }

    object Otel {
        private const val oTelInstrumentationBomAlpha = "2.15.0-alpha"
        const val otelAndroidBomVersion = "0.11.0-alpha"
        const val instrumentationBomAlpha = "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$oTelInstrumentationBomAlpha"
        const val androidBom = "io.opentelemetry.android:opentelemetry-android-bom:$otelAndroidBomVersion"

        const val api = "io.opentelemetry:opentelemetry-api"
        const val sdk = "io.opentelemetry:opentelemetry-sdk"
        const val exporterOtlpCommon = "io.opentelemetry:opentelemetry-exporter-otlp-common"
        const val exporterOtlp = "io.opentelemetry:opentelemetry-exporter-otlp"
        const val semConv = "io.opentelemetry.semconv:opentelemetry-semconv"
        const val semConvIncubating = "io.opentelemetry.semconv:opentelemetry-semconv-incubating"

        const val instrumentationApi = "io.opentelemetry.instrumentation:opentelemetry-instrumentation-api"
        const val instrumentationOkHttp3Library = "io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0"

        const val androidCore = "io.opentelemetry.android:core"
        const val androidSession = "io.opentelemetry.android:session"
        const val androidServices = "io.opentelemetry.android:services"
        const val androidCommon = "io.opentelemetry.android:common"
        const val androidInstrumentation = "io.opentelemetry.android.instrumentation:android-instrumentation"
        const val androidHttpUrlLibrary = "io.opentelemetry.android.instrumentation:httpurlconnection-library"
        const val androidOkHttp3Library = "io.opentelemetry.android.instrumentation:okhttp3-library"
        const val androidNetworkMonitorInstrumentation = "io.opentelemetry.android.instrumentation:network"
        const val androidCrashInstrumentation = "io.opentelemetry.android.instrumentation:crash"
        const val androidANRInstrumentation = "io.opentelemetry.android.instrumentation:anr"
        const val androidSlowRenderingInstrumentation = "io.opentelemetry.android.instrumentation:slowrendering"
    }

    object AndroidTest {
        private const val junitVersion = "1.1.3"
        private const val okhttpLoggingVersion = "4.12.0"
        private const val serializationVersion = "1.5.0"
        private const val testExtTruthVersion = "1.6.0-alpha03"
        private const val testOrchestratorVersion = "1.4.2"
        private const val testRulesVersion = "1.4.0"
        private const val testRunnerVersion = "1.5.2"
        private const val uiAutomatorVersion = "2.2.0"
        private const val mockkVersion = "1.12.4"

        const val junit = "androidx.test.ext:junit:$junitVersion"
        const val okhttpLogging = "com.squareup.okhttp3:logging-interceptor:$okhttpLoggingVersion"
        const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion"
        const val testExtTruth = "androidx.test.ext:truth:$testExtTruthVersion"
        const val testOrchestrator = "androidx.test:orchestrator:$testOrchestratorVersion"
        const val testRules = "androidx.test:rules:$testRulesVersion"
        const val testRunner = "androidx.test:runner:$testRunnerVersion"
        const val uiAutomator = "androidx.test.uiautomator:uiautomator:$uiAutomatorVersion"
        const val mockk = "io.mockk:mockk-android:$mockkVersion"

        object Espresso {

            /**
             * Espresso contrib 3.5.1 is the latest version.
             * This version includes jsoup transitively with vulnerabilities, we explicitly set the jsoup version to a safer one.
             */
            private const val contribVersion = "3.5.1"
            private const val jsoupVersion = "1.15.3"

            private const val coreVersion = "3.5.1"
            private const val idlingConcurrentVersion = "3.5.1"
            private const val idlingResourceVersion = ""
            private const val intentsVersion = "3.5.1"
            private const val webVersion = "3.5.1"

            const val contrib = "androidx.test.espresso:espresso-contrib:$contribVersion"
            const val jsoup = "org.jsoup:jsoup:$jsoupVersion"
            const val core = "androidx.test.espresso:espresso-core:$coreVersion"
            const val idlingConcurrent = "androidx.test.espresso.idling:idling-concurrent:$idlingConcurrentVersion"
            const val idlingResource = "androidx.test.espresso:espresso-idling-resource:$idlingResourceVersion"
            const val intents = "androidx.test.espresso:espresso-intents:$intentsVersion"
            const val web = "androidx.test.espresso:espresso-web:$webVersion"
        }
    }

    object AndroidDebug {
        private const val leakCanaryVersion = "2.12"

        const val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
    }
}
