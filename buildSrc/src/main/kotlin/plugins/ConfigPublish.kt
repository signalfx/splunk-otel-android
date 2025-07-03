package plugins

import addCiscoInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering
import org.gradle.plugins.signing.SigningExtension
import toBoxString
import utils.artifactIdProperty
import utils.defaultGroupId
import utils.versionProperty

class ConfigPublish : Plugin<Project> by local plugin {
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
//    apply(plugin = "org.jetbrains.dokka")

    afterEvaluate {
        val sourcesJar by tasks.creating(Jar::class) {
            archiveClassifier.set("sources")
        }

        // TODO: Implement this using dokka -> needs exception in BlackDuck
        val androidJavadocsJar by tasks.registering(org.gradle.api.tasks.bundling.Jar::class) {
//            dependsOn(tasks["dokkaJavadoc"])
//            from(File(buildDir, "dokka/javadoc"))
            from("$rootDir/README.md")
            archiveClassifier.set("javadoc")
            metaInf {
                from("$projectDir/src/main/assets/LICENSE.txt")
            }
        }

        configure<SigningExtension> {
            val signingKey: String? = project.findProperty("signingKey") as String?
            val signingPassword: String? = project.findProperty("signingPassword") as String?

            if (signingKey != null && signingPassword != null) {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(project.extensions.getByType(PublishingExtension::class.java).publications)
            } else {
                println("WARNING: Environment variables signingKey and/or signingPassword not set. Skipping signing of artifacts.")
            }
        }

        configure<PublishingExtension> {
            publications {
                register("maven", MavenPublication::class) {
                    from(components["release"])
                    groupId = defaultGroupId
                    artifactId = project.properties[artifactIdProperty].toString()

                    val baseVersion = project.properties[versionProperty].toString()
                    version = if (project.findProperty("release") != "true") {
                        "$baseVersion-SNAPSHOT"
                    } else {
                        baseVersion
                    }

                    artifact(sourcesJar)
                    artifact(androidJavadocsJar)
                    pom.withXml { asNode().addCiscoInfo() }
                }
            }
            repositories {
                mavenLocal()
            }
        }
    }

    tasks.withType(AbstractPublishToMaven::class.java) {
        doLast {
            // Using actual publication version instead of the property to include potential -SNAPSHOT suffix
            val publication = project.extensions.getByType<PublishingExtension>()
                .publications.getByName("maven") as MavenPublication
            val artifact = "$defaultGroupId:${project.properties[artifactIdProperty]}:${publication.version}"

            println("╔══════════════════════════════════════════════════════════════════════════════════════════════════╗")
            println("published".toBoxString())
            println(artifact.toBoxString())
            println("╚══════════════════════════════════════════════════════════════════════════════════════════════════╝")
        }
    }
}
