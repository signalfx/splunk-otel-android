package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class ConfigKtLint : Plugin<Project> by local plugin {
    val ktlint by configurations.creating

    dependencies {
        ktlint(Dependencies.ktlint) {
            attributes {
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            }
        }
    }

    val outputDir = Ktlint.outputDir(project.buildDir)
    val inputFiles = project.fileTree(mapOf("dir" to Ktlint.INPUT_DIR, "include" to Ktlint.INCLUDED_FILES))

    tasks.register<JavaExec>("ktlintCheck") {
        inputs.files(inputFiles)
        outputs.dir(outputDir)

        group = TaskGroups.VERIFICATION
        description = "Check Kotlin code style."
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        args = listOf("${Ktlint.INPUT_DIR}/${Ktlint.INCLUDED_FILES}")
    }

    tasks.register<JavaExec>("ktlintFormat") {
        inputs.files(inputFiles)
        outputs.dir(outputDir)

        group = TaskGroups.FORMATTING
        description = "Fix Kotlin code style deviations."
        classpath = ktlint
        mainClass.set("com.pinterest.ktlint.Main")
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
        args = listOf("-F", "${Ktlint.INPUT_DIR}/${Ktlint.INCLUDED_FILES}")
    }
}
