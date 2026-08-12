/**
 * This object holds all the dependencies for the MRUM Agent.
 * The example application dependencies can be found in AppDependencies.kt.
 */
object Dependencies {

    // Project level dependencies

    private const val gradleVersion = "8.6.0"
    private const val kotlinVersion = "1.8.0"
    private const val ktlintVersion = "1.7.1"
    private const val bytebuddyVersion = "1.18.8"
    const val jacocoVersion = "0.8.15"

    const val gradle = "com.android.tools.build:gradle:$gradleVersion"
    const val gradleApi = "com.android.tools.build:gradle-api:$gradleVersion"
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
        private const val androidXTestRunnerVersion = "1.6.2"
        private const val robolectricVersion = "4.13"
        private const val mockitoVersion = "5.4.0"

        const val junit = "junit:junit:$junitVersion"
        const val androidXTestCore = "androidx.test:core:$androidXTestCoreVersion"
        const val androidXTestJunit = "androidx.test.ext:junit:$androidXTestJunitVersion"
        const val androidXTestRunner = "androidx.test:runner:$androidXTestRunnerVersion"
        const val robolectric = "org.robolectric:robolectric:$robolectricVersion"
        const val mockito = "org.mockito:mockito-core:$mockitoVersion"
    }

    object Android {

        private const val annotationVersion = "1.9.1"
        private const val fragmentKtxVersion = "1.3.3"
        private const val navigationVersion = "2.4.0"

        const val annotation = "androidx.annotation:annotation:$annotationVersion"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentKtxVersion"
        const val navigationRuntime = "androidx.navigation:navigation-runtime:$navigationVersion"

        object Compose {
            private const val UiVersion = "1.2.1" // No need to update

            const val ui = "androidx.compose.ui:ui:$UiVersion"
        }
    }

    object Common {
        private const val version = "1.0.1"

        const val http = "com.splunk:rum-common-http:$version"
        const val job = "com.splunk:rum-common-job:$version"
        const val storage = "com.splunk:rum-common-storage:$version"
        const val utils = "com.splunk:rum-common-utils:$version"
        const val logger = "com.splunk:rum-common-logger:$version"
    }

    object SessionReplay {
        private const val version = "1.1.6"

        const val instrumentationSessionRecordingCore = "com.splunk.android:sr-instrumentation-session-recording-core:$version"
        const val instrumentationSessionRecordingFrameCapturer = "com.splunk.android:sr-instrumentation-session-recording-frame-capturer:$version"
        const val instrumentationSessionRecordingInteractions = "com.splunk.android:sr-instrumentation-session-recording-interactions:$version"
    }

    object Otel {
        private const val oTelInstrumentationBomAlpha = "2.15.0-alpha"
        const val instrumentationBomAlpha = "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$oTelInstrumentationBomAlpha"

        const val api = "io.opentelemetry:opentelemetry-api"
        const val sdk = "io.opentelemetry:opentelemetry-sdk"
        const val exporterOtlpCommon = "io.opentelemetry:opentelemetry-exporter-otlp-common"
        const val exporterOtlp = "io.opentelemetry:opentelemetry-exporter-otlp"
        const val semConv = "io.opentelemetry.semconv:opentelemetry-semconv"
        const val semConvIncubating = "io.opentelemetry.semconv:opentelemetry-semconv-incubating"

        const val instrumentationApi = "io.opentelemetry.instrumentation:opentelemetry-instrumentation-api"
        const val instrumentationApiIncubator =
            "io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator"
    }
}
