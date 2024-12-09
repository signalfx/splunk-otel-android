import plugins.ConfigAndroidLibrary
import plugins.ConfigPublish
import utils.artifactIdProperty
import utils.artifactPrefix
import utils.commonPrefix
import utils.versionProperty

plugins {
	id("com.android.library")
	id("kotlin-android")
}

apply<ConfigAndroidLibrary>()
apply<ConfigPublish>()

ext {
	set(artifactIdProperty, "$artifactPrefix$commonPrefix${project.name}")
    set(versionProperty, Configurations.sdkVersionName)
}

android {
	namespace = "com.smartlook.sdk.common.utils"
}

dependencies {
    implementation(Dependencies.kotlinStdlibJdk8)

    compileOnly(Dependencies.Android.annotation)
	compileOnly(Dependencies.Android.fragmentKtx)
	compileOnly(Dependencies.Android.recycler)
}
