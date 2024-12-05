buildscript {
    repositories {
        mavenCentral()
        google()

        maven {
            url = uri("$projectDir/gradle-plugin/obfuscation-director/repository")
        }

        maven {
            url = uri("$projectDir/gradle-plugin/documentation-extractor/repository")
        }
    }

    dependencies {
        classpath(Dependencies.gradle)
        classpath(Dependencies.gradleApi)
        classpath(Dependencies.buildInfoExtractorGradle)
        classpath(Dependencies.kotlin)
    }
}

allprojects {
    apply<plugins.ConfigKtLint>()

    repositories {
        mavenLocal()
        mavenCentral()
        google()

        maven {
            setUrl(Configurations.Artifactory.repositoryURL)

            /**
             * Create a file gradle.properties in the GRADLE_USER_HOME directory.
             * By default this is in the USER_HOME/.gradle directory.
             * Insert following lines into gradle.properties:
             * sl_artifactory_token = <token>
             */
            credentials(HttpHeaderCredentials::class) {
                name = "Authorization"
                value = "Bearer ${project.findProperty("sl_artifactory_token")}"
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}