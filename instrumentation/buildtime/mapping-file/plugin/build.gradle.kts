import utils.artifactPrefix
import utils.defaultGroupId

private val pluginName = "mapping-file-plugin"

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
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Version" to Dependencies.Otel.otelAndroidBomVersion
        )
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}

gradlePlugin {
    plugins {
        create("androidInstrumentationPlugin") {
            id = "$defaultGroupId.${artifactPrefix}$pluginName"
            implementationClass = "com.splunk.rum.mappingfile.plugin.MappingFilePlugin"
            displayName = "Splunk Android Mapping File Plugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation(Dependencies.gradle)
}

signing {
    val signingKey: String? = project.findProperty("signingKey") as String?
    val signingPassword: String? = project.findProperty("signingPassword") as String?

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        println("WARNING: Environment variables signingKey and/or signingPassword not set. Skipping signing of artifacts.")
    }
}

publishing {
    publications {
        withType(MavenPublication::class.java) {
            pom.withXml { asNode().addSplunkInfo() }

            artifactId = "${artifactPrefix}$pluginName"

            artifact(javadocJar)
            artifact(sourcesJar)
        }
        repositories {
            maven {
                name = "local"
                url = uri("$projectDir/repo")
            }
        }
    }
}