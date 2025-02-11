buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath(Dependencies.gradle)
        classpath(Dependencies.buildInfoExtractorGradle)
        classpath(Dependencies.kotlin)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    }
}

allprojects {
    apply<plugins.ConfigKtLint>()

    repositories {
        mavenLocal()
        mavenCentral()
        google()

        maven {
            setUrl("https://sdk.smartlook.com/android/release")
        }
    }
}