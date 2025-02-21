import org.gradle.api.JavaVersion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

object Configurations {

    object Android {
        const val appCompileVersion = 34
        const val compileVersion = 34
        const val minVersion = 24
        const val targetVersion = 34
    }

    object Compilation {
        const val jvmTarget = "1.8"
        val sourceCompatibility = JavaVersion.VERSION_1_8
        val targetCompatibility = JavaVersion.VERSION_1_8
    }

    object Artifactory {
        const val bareRepositoryURL = "https://artifactory.bare.appdynamics.com/artifactory/maven-releases/"
    }

    const val sdkVersionCode = 1
    val sdkVersionName = "24.4.1" // version()

    /**
     * TODO: Uncomment this and use the "dynamic" version after the release; otherwise, the merge to master will not work.
     */
    private fun version(): String {
        val format = SimpleDateFormat("yy.M.d-HHmm").apply { timeZone = TimeZone.getTimeZone("UTC") }
        return format.format(Date(System.currentTimeMillis()))
    }
}
