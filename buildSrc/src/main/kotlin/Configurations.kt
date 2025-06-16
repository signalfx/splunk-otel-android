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
    val sdkVersionName = "2.0.0-beta"
}
