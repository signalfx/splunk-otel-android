import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.networkRequestPrefix
import utils.versionProperty

plugins {
    id("com.android.library")
    id("kotlin-android")
}

apply<ConfigPublish>()

ext {
    set(artifactIdProperty, "$artifactPrefix${networkRequestPrefix}${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
    namespace = "com.splunk.rum.networkrequest.library"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = Configurations.Compilation.sourceCompatibility
        targetCompatibility = Configurations.Compilation.targetCompatibility
    }

    lint {
        compileSdk = 24
    }
}

dependencies {
    coreLibraryDesugaring(Dependencies.desugar)
    api(Dependencies.Otel.context)
    api(Dependencies.Otel.api)
    implementation(Dependencies.Otel.instrumentationSemConv)
    implementation(Dependencies.Otel.instrumentationApi)
    implementation(Dependencies.Otel.instrumentationOkhttp3)
    implementation(project(":common:otel:api"))
    implementation(project(":integration:networkrequest"))
    compileOnly(Dependencies.okhttp)
    compileOnly(Dependencies.okio)

    implementation(Dependencies.kotlinStdlibJdk8)

    //Test Dependencies
    testImplementation(Dependencies.Test.mockk)
    testImplementation(Dependencies.Test.junit)
    testImplementation(Dependencies.okhttp)
    testImplementation(Dependencies.okio)
}
