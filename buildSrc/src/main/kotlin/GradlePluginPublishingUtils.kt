import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension
import utils.defaultGroupId

fun Project.createStandardJars() {
    tasks.create("javadocJar", Jar::class) {
        archiveClassifier.set("javadoc")
        from(tasks.named("javadoc"))
    }

    tasks.create("sourcesJar", Jar::class) {
        archiveClassifier.set("sources")
        from(provider {
            project.extensions.getByType<SourceSetContainer>()["main"].allSource
        })
    }

    tasks.named<Jar>("jar") {
        manifest {
            attributes(
                "Implementation-Version" to Dependencies.Otel.otelAndroidBomVersion
            )
        }
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}

fun Project.configureGradlePluginPublishing(artifactIdValue: String) {
    extensions.configure<PublishingExtension> {
        publications {
            withType<MavenPublication> {
                pom.withXml { asNode().addSplunkInfo() }
                artifactId = artifactIdValue
                artifact(tasks.named("javadocJar"))
                artifact(tasks.named("sourcesJar"))
            }
        }
        repositories {
            mavenLocal()
        }
    }
}

fun Project.configureGradlePluginSigning() {
    extensions.configure<SigningExtension> {
        val signingKey: String? = project.findProperty("signingKey") as String?
        val signingPassword: String? = project.findProperty("signingPassword") as String?

        if (signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(project.extensions.getByType<PublishingExtension>().publications)
        } else {
            println("WARNING: Environment variables signingKey and/or signingPassword not set. Skipping signing of artifacts.")
        }
    }
}

fun Project.addPublishingOutput() {
    tasks.withType<AbstractPublishToMaven>().configureEach {
        doLast {
            val artifact = "$defaultGroupId:${publication.artifactId}:${publication.version}"

            println("╔══════════════════════════════════════════════════════════════════════════════════════════════════╗")
            println("published".toBoxString())
            println(artifact.toBoxString())
            println("╚══════════════════════════════════════════════════════════════════════════════════════════════════╝")
        }
    }
}