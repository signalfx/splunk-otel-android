/**
 * This object holds all the dependencies for the MRUM Agent.
 * The example application dependencies can be found in AppDependencies.kt.
 */
object Dependencies {

    // Project level dependencies

    private const val gradleVersion = "8.6.0"
    private const val kotlinVersion = "1.8.0"
    private const val ktlintVersion = "1.7.1"
    private const val bytebuddyVersion = "1.17.2"
    const val jacocoVersion = "0.8.13"

    const val gradle = "com.android.tools.build:gradle:$gradleVersion"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    const val ktlint = "com.pinterest.ktlint:ktlint-cli:$ktlintVersion"
    const val jacoco = "org.jacoco:org.jacoco.core:$jacocoVersion"

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

    const val okhttp = "com.squareup.okhttp3:okhttp:$okhttpVersion"
    const val okio = "com.squareup.okio:okio:$okioVersion"
    const val bytebuddy = "net.bytebuddy:byte-buddy:$bytebuddyVersion"
    const val bytebuddyGradlePlugin = "net.bytebuddy:byte-buddy-gradle-plugin:$bytebuddyVersion"

    object Test {
        private const val junitVersion = "4.13.2"
        private const val androidXTestCoreVersion = "1.6.1"
        private const val androidXTestJunitVersion = "1.2.1"
        private const val robolectricVersion = "4.13"
        private const val mockitoVersion = "5.4.0"

        const val junit = "junit:junit:$junitVersion"
        const val androidXTestCore = "androidx.test:core:$androidXTestCoreVersion"
        const val androidXTestJunit = "androidx.test.ext:junit:$androidXTestJunitVersion"
        const val robolectric = "org.robolectric:robolectric:$robolectricVersion"
        const val mockito = "org.mockito:mockito-core:$mockitoVersion"
    }

    object Android {

        private const val annotationVersion = "1.9.1"
        private const val fragmentKtxVersion = "1.3.3"

        const val annotation = "androidx.annotation:annotation:$annotationVersion"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentKtxVersion"

        object Compose {
            private const val UiVersion = "1.2.1" // No need to update

            const val ui = "androidx.compose.ui:ui:$UiVersion"
        }
    }

    object SessionReplay {
        private const val version = "1.0.25"

        const val commonHttp = "com.splunk.android:sr-common-http:$version"
        const val commonId = "com.splunk.android:sr-common-id:$version"
        const val commonJob = "com.splunk.android:sr-common-job:$version"
        const val commonStorage = "com.splunk.android:sr-common-storage:$version"
        const val commonUtils = "com.splunk.android:sr-common-utils:$version"
        const val commonLogger = "com.splunk.android:sr-common-logger:$version"

        const val instrumentationSessionRecordingCore = "com.splunk.android:sr-instrumentation-session-recording-core:$version"
        const val instrumentationSessionRecordingFrameCapturer = "com.splunk.android:sr-instrumentation-session-recording-frame-capturer:$version"
        const val instrumentationSessionRecordingInteractions = "com.splunk.android:sr-instrumentation-session-recording-interactions:$version"
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
}
