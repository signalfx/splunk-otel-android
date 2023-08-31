import org.gradle.api.publish.maven.MavenPublication
import java.net.URI

plugins {
    id("com.android.library")
    id("maven-publish")
    id("signing")
}

android.lint {
    warningsAsErrors = true
    // A newer version of androidx.appcompat:appcompat than 1.3.1 is available: 1.4.1 [GradleDependency]
    // we rely on dependabot for dependency updates
    disable.add("GradleDependency")
}

val isARelease = project.hasProperty("release") && project.property("release") == "true"
val variantToPublish = "release"

android.publishing {
    singleVariant(variantToPublish) {
        // Adding sources and javadoc artifacts only during a release.
        if (isARelease) {
            withJavadocJar()
            withSourcesJar()
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            val snapshotsRepoUrl = URI("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (project.findProperty("release") == "true") releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = findProperty("mavenCentralUsername") as String?
                password = findProperty("mavenCentralPassword") as String?
            }
        }
    }
}



project.afterEvaluate {
    val javadoc by tasks.registering(Javadoc::class) {
        source = android.sourceSets.named("main").get().java.getSourceFiles()
        classpath += project.files(android.bootClasspath)

        // grab the library variants, because apparently this is where the real classpath lives that
        // is needed for javadoc generation.
        val firstVariant = project.android.libraryVariants.toList().first()
        val javaCompile = firstVariant.javaCompileProvider.get()
        classpath += javaCompile.classpath
        classpath += javaCompile.outputs.files
    }
    publishing.publications {
        create<MavenPublication>("maven") {
            from(components.findByName(variantToPublish))
            groupId = "com.splunk"
            artifactId = base.archivesName.get()

            afterEvaluate {
                pom.name.set("${project.extra["pomName"]}")
                pom.description.set(project.description)
            }

            pom {
                url.set("https://github.com/signalfx/splunk-otel-android")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("splunk")
                        name.set("Splunk Instrumentation Authors")
                        email.set("support+java@signalfx.com")
                        organization.set("Splunk")
                        organizationUrl.set("https://www.splunk.com")
                    }
                }
                scm {
                    connection.set("https://github.com/signalfx/splunk-otel-android.git")
                    developerConnection.set("https://github.com/signalfx/splunk-otel-android.git")
                    url.set("https://github.com/signalfx/splunk-otel-android")
                }
            }
        }
    }
    if (isARelease && project.findProperty("skipSigning") != "true") {
        signing {
            useGpgCmd()
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["maven"])
        }
    }
}
