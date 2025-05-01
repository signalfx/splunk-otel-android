import utils.artifactPrefix
import utils.defaultGroupId

plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    id("maven-publish")
    id("signing")
}

group = defaultGroupId
version = Configurations.sdkVersionName

val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.named("javadoc"))
    metaInf {
        from("$projectDir/src/main/assets/LICENSE.txt")
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
    metaInf {
        from("$projectDir/src/main/assets/LICENSE.txt")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Version" to Dependencies.Otel.otelAndroidBomVersion
        )
    }
    metaInf {
        from("$projectDir/src/main/assets/LICENSE.txt")
    }
}

gradlePlugin {
    plugins {
        create("androidInstrumentationPlugin") {
            id = "$defaultGroupId.${artifactPrefix}okhttp3-auto-plugin"
            implementationClass = "com.splunk.rum.okhttp3.auto.plugin.OkHttp3InstrumentationPlugin"
            displayName = "Splunk Android OkHttp3 Auto-Instrumentation Plugin"
        }
    }
}

dependencies {
    implementation(Dependencies.bytebuddyGradlePlugin)
    implementation(Dependencies.bytebuddy)
    implementation(gradleApi())
}

signing {
    val secretKey: String? = System.getenv("SECRET_KEY")
    val signingPassword: String? = System.getenv("SIGNING_PASSWORD")

    if (secretKey != null && signingPassword != null) {
        useInMemoryPgpKeys(secretKey, signingPassword)
        sign(publishing.publications)
    } else {
        println("WARNING: Environment variables SECRET_KEY and/or SIGNING_PASSWORD not set. Skipping signing of artifacts.")
    }
}

publishing {
    publications {
        withType(MavenPublication::class.java) {
            pom.withXml { asNode().addCiscoInfo() }

            artifactId = "${artifactPrefix}okhttp3-auto-plugin"

            artifact(javadocJar)
            artifact(sourcesJar)
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
