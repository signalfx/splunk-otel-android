import org.gradle.api.JavaVersion

object Configurations {

    object Android {
        const val appCompileVersion = 34
        const val compileVersion = 34
        const val minVersion = 21
        const val targetVersion = 34
    }

    object Compilation {
        const val jvmTarget = "1.8"
        val sourceCompatibility = JavaVersion.VERSION_1_8
        val targetCompatibility = JavaVersion.VERSION_1_8
    }

    const val sdkVersionCode = 1
    val sdkVersionName = "2.1.7"
}
