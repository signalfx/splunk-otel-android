package plugins

import addCiscoInfo
import getVersionPostfix
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
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering
import org.gradle.plugins.signing.SigningExtension
import toBoxString
import utils.artifactIdProperty
import utils.defaultGroupId
import utils.versionProperty
import java.io.File

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
            val secretKey: String? = System.getenv("SECRET_KEY")
            val signingPassword: String? = System.getenv("SIGNING_PASSWORD")

            if (secretKey != null && signingPassword != null) {
                useInMemoryPgpKeys(secretKey, signingPassword)
                sign(project.extensions.getByType(PublishingExtension::class.java).publications)
            } else {
                println("WARNING: Environment variables SECRET_KEY and/or SIGNING_PASSWORD not set. Skipping signing of artifacts.")
            }
        }

        configure<PublishingExtension> {
            publications {
                register("release", MavenPublication::class) {
                    from(components["release"])
                    groupId = defaultGroupId
                    artifactId = project.properties[artifactIdProperty].toString()
                    version = "${project.properties[versionProperty]}${project.getVersionPostfix()}"

                    artifact(sourcesJar)
                    artifact(androidJavadocsJar)
                    pom.withXml { asNode().addCiscoInfo() }
                }
            }
            repositories {
                maven {
                    name = "maven"
                    url = uri(Configurations.Artifactory.bareRepositoryURL)
                    credentials {
                        username = System.getenv("ARTIFACT_REPO_USERNAME")
                        password = System.getenv("ARTIFACT_REPO_PASSWORD")
                    }
                }
                maven {
                    name = "local"
                    url = uri("$projectDir/repo")
                }
            }
        }
    }

    tasks.withType(AbstractPublishToMaven::class.java) {
        doLast {
            val artifact = "$defaultGroupId:${project.properties[artifactIdProperty]}:${project.properties[versionProperty]}${getVersionPostfix()}"
            println("╔══════════════════════════════════════════════════════════════════════════════════════════════════╗")
            println("published".toBoxString())
            println(artifact.toBoxString())
            println("╚══════════════════════════════════════════════════════════════════════════════════════════════════╝")
        }
    }
}
