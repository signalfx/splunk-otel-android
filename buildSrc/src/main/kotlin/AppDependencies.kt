object AppDependencies {

    const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0"
    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.14"


    /**
     * OkHttp 4.11.0 is the latest version that does not mandate the use of Kotlin 1.9.0.
     * Since this version includes Okio transitively with vulnerabilities, we explicitly set the Okio version to a safer one.
     */
    private const val okhttpVersion = "4.11.0"
    private const val okioVersion = "3.4.0"

    const val okhttp = "com.squareup.okhttp3:okhttp:$okhttpVersion"
    const val okio = "com.squareup.okio:okio:$okioVersion"

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

    object Android {
        private const val appcompatVersion = "1.7.1"
        private const val constraintLayoutVersion = "2.2.1"
        private const val activityKtxVersion = "1.2.2"
        private const val fragmentKtxVersion = "1.3.3"
        private const val materialVersion = "1.9.0"

        const val appcompat = "androidx.appcompat:appcompat:$appcompatVersion"
        const val constraintLayout = "androidx.constraintlayout:constraintlayout:$constraintLayoutVersion"
        const val activityKtx = "androidx.activity:activity-ktx:$activityKtxVersion"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentKtxVersion"
        const val material = "com.google.android.material:material:$materialVersion"
    }

    object Test {
        private const val junitVersion = "1.3.0"
        private const val okhttpLoggingVersion = "4.12.0"
        private const val serializationVersion = "1.5.0"
        private const val testExtTruthVersion = "1.7.0"
        private const val testOrchestratorVersion = "1.6.1"
        private const val testRulesVersion = "1.7.0"
        private const val testRunnerVersion = "1.7.0"
        private const val uiAutomatorVersion = "2.3.0"
        private const val mockkVersion = "1.12.4"
        private const val jsonassertVersion = "1.5.3"

        const val junit = "androidx.test.ext:junit:$junitVersion"
        const val okhttpLogging = "com.squareup.okhttp3:logging-interceptor:$okhttpLoggingVersion"
        const val serialization = "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion"
        const val testExtTruth = "androidx.test.ext:truth:$testExtTruthVersion"
        const val testOrchestrator = "androidx.test:orchestrator:$testOrchestratorVersion"
        const val testRules = "androidx.test:rules:$testRulesVersion"
        const val testRunner = "androidx.test:runner:$testRunnerVersion"
        const val uiAutomator = "androidx.test.uiautomator:uiautomator:$uiAutomatorVersion"
        const val mockk = "io.mockk:mockk-android:$mockkVersion"
        const val jsonassert = "org.skyscreamer:jsonassert:$jsonassertVersion"

        object Espresso {

            /**
             * Espresso contrib 3.5.1 is the latest version.
             * This version includes jsoup transitively with vulnerabilities, we explicitly set the jsoup version to a safer one.
             */
            private const val contribVersion = "3.7.0"
            private const val jsoupVersion = "1.15.3"

            private const val coreVersion = "3.7.0"
            private const val idlingConcurrentVersion = "3.7.0"
            private const val idlingResourceVersion = ""
            private const val intentsVersion = "3.7.0"
            private const val webVersion = "3.7.0"

            const val contrib = "androidx.test.espresso:espresso-contrib:$contribVersion"
            const val jsoup = "org.jsoup:jsoup:$jsoupVersion"
            const val core = "androidx.test.espresso:espresso-core:$coreVersion"
            const val idlingConcurrent = "androidx.test.espresso.idling:idling-concurrent:$idlingConcurrentVersion"
            const val idlingResource = "androidx.test.espresso:espresso-idling-resource:$idlingResourceVersion"
            const val intents = "androidx.test.espresso:espresso-intents:$intentsVersion"
            const val web = "androidx.test.espresso:espresso-web:$webVersion"
        }
    }

}