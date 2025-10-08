package plugins

import addSplunkInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import toBoxString
import utils.defaultGroupId

// Defining property keys for customization by individual Gradle Plugin Modules
const val pluginIdSuffixProperty = "pluginIdSuffix"
const val pluginImplementationClassProperty = "pluginImplementationClass"
const val pluginDisplayNameProperty = "pluginDisplayName"
const val pluginArtifactIdProperty = "pluginArtifactId"

class ConfigPlugin : Plugin<Project> by local plugin {
        apply(plugin = "java-gradle-plugin")
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        group = defaultGroupId
        version = Configurations.sdkVersionName

        val javadocJar by tasks.creating(Jar::class) {
            archiveClassifier.set("javadoc")
            from(tasks.named("javadoc"))
        }

        val sourcesJar by tasks.creating(Jar::class) {
            archiveClassifier.set("sources")
            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            from(sourceSets["main"].allSource)
        }

        tasks.named<Jar>("jar") {
            manifest {
                attributes(
                    "Implementation-Version" to Dependencies.Otel.otelAndroidBomVersion
                )
            }
        }

        tasks.withType(PublishToMavenRepository::class.java).configureEach {
            dependsOn(tasks.withType(Sign::class.java))
        }

        afterEvaluate {
            // Configure gradle plugin based on the ext values set in each Gradle Plugin build.gradle.kts
            configure<GradlePluginDevelopmentExtension> {
                plugins {
                    create("androidInstrumentationPlugin") {
                        val pluginIdSuffix = project.properties[pluginIdSuffixProperty]?.toString()
                            ?: error("Property '$pluginIdSuffixProperty' must be set")
                        val implementationClass = project.properties[pluginImplementationClassProperty]?.toString()
                            ?: error("Property '$pluginImplementationClassProperty' must be set")
                        val displayName = project.properties[pluginDisplayNameProperty]?.toString()
                            ?: error("Property '$pluginDisplayNameProperty' must be set")

                        id = "$defaultGroupId.$pluginIdSuffix"
                        this.implementationClass = implementationClass
                        this.displayName = displayName
                    }
                }
            }

            configure<SigningExtension> {
                val signingKey: String? = project.findProperty("signingKey") as String?
                val signingPassword: String? = project.findProperty("signingPassword") as String?

                if (signingKey != null && signingPassword != null) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                    sign(project.extensions.getByType<PublishingExtension>().publications)
                } else {
                    println("WARNING: Environment variables signingKey and/or signingPassword not set. Skipping signing of artifacts.")
                }
            }

            configure<PublishingExtension> {
                publications {
                    withType(MavenPublication::class.java) {
                        pom.withXml { asNode().addSplunkInfo() }

                        val artifactId = project.properties[pluginArtifactIdProperty]?.toString()
                            ?: error("Property '$pluginArtifactIdProperty' must be set")
                        this.artifactId = artifactId

                        artifact(javadocJar)
                        artifact(sourcesJar)
                    }
                }
                repositories {
                    mavenLocal()
                    maven {
                        name = "local"
                        url = uri("$projectDir/repo")
                    }
                }
            }
        }

        tasks.withType(AbstractPublishToMaven::class.java) {
            doLast {
                val artifact = "$defaultGroupId:${publication.artifactId}:${publication.version}"

                println("╔══════════════════════════════════════════════════════════════════════════════════════════════════╗")
                println("published!".toBoxString())
                println(artifact.toBoxString())
                println("╚══════════════════════════════════════════════════════════════════════════════════════════════════╝")
            }
        }
}