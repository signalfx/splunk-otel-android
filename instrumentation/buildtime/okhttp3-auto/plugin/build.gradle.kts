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

tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
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

            artifactId = "${artifactPrefix}okhttp3-auto-plugin"

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
