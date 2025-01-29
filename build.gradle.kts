buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath(Dependencies.gradle)
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
            setUrl("https://sdk.smartlook.com/android/release")
        }
    }
}