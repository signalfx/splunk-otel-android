package plugins

import TaskGroups
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.tasks.JacocoReport

class ConfigJacoco : Plugin<Project> by local plugin {
    apply(plugin = "jacoco")

    tasks.register<JacocoReport>("jacocoTestReport") {
        dependsOn("testDebugUnitTest")
        group = TaskGroups.VERIFICATION

        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        executionData.setFrom(
            fileTree(buildDir.resolve("outputs/unit_test_code_coverage/debugUnitTest")) {
                include("*.exec")
            }
        )

        classDirectories.setFrom(
            files(
                fileTree(
                    "$buildDir/intermediates/javac/debug/classes"
                ) {
                    exclude(
                        "/R.class", "/R$.class", "/BuildConfig.",
                        "/Manifest.", "/Test.",
                    )
                },
                fileTree("${buildDir}/tmp/kotlin-classes/debug") {
                    exclude(
                        "/R.class", "/R$.class", "*/Test."
                    )
                }
            )
        )

        sourceDirectories.setFrom(
            files(
                "$projectDir/src/main/java",
                "$projectDir/src/main/kotlin",
                "$projectDir/src/debug/java",
                "$projectDir/src/debug/kotlin"
            )
        )
    }
}
