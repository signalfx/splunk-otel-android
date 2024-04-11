plugins {
    `kotlin-dsl`

    // When updating, update below in dependencies too
    id("com.diffplug.spotless") version "6.25.0"
}

spotless {
    kotlinGradle {
        ktlint()
        target("* * / *.gradle.kts")
    }
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.plugin)
    implementation(libs.spotless.plugin)
    implementation(libs.errorprone.plugin)
    implementation(libs.nullaway.plugin)
}
