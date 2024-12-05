package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project

class ConfigLint : Plugin<Project> by local plugin {
    android {
        lint {
            baseline = file("lint-baseline.xml")
        }
    }
}
