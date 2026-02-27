/**
 * This object holds all the dependencies for the example application.
 * The Agent dependencies can be found in Dependencies.kt.
 */
object AppDependencies {

    private const val kotlinStdlibVersion = "1.8.0"
    private const val leakCanaryVersion = "2.14"
    private const val desugarVersion = "2.1.5"

    const val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinStdlibVersion"
    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:$leakCanaryVersion"
    const val desugar = "com.android.tools:desugar_jdk_libs:$desugarVersion"


    /**
     * OkHttp 4.11.0 is the latest version that does not mandate the use of Kotlin 1.9.0.
     * Since this version includes Okio transitively with vulnerabilities, we explicitly set the Okio version to a safer one.
     */
    private const val okhttpVersion = "4.11.0"
    private const val okioVersion = "3.4.0"

    const val okhttp = "com.squareup.okhttp3:okhttp:$okhttpVersion"
    const val okio = "com.squareup.okio:okio:$okioVersion"

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
}