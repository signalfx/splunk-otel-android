package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.findByType
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

@Suppress("ClassName")
internal object local

internal infix fun local.plugin(config: Project.() -> Unit) = Plugin<Project> { config(it) }

internal fun Project.androidApplication(block: ApplicationExtension.() -> Unit) {
    extensions.findByType<ApplicationExtension>()?.apply(block)
}

internal fun Project.androidLibrary(block: LibraryExtension.() -> Unit) {
    extensions.findByType<LibraryExtension>()?.apply(block)
}

internal fun Project.android(block: LibraryExtension.() -> Unit) {
    extensions.findByType<LibraryExtension>()?.apply(block)
}

internal fun Project.java(block: JavaPluginExtension.() -> Unit) {
    extensions.findByType<JavaPluginExtension>()?.apply(block)
}

internal fun Project.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    tasks.withType<KotlinJvmCompile> {
        kotlinOptions {
            block()
        }
    }
}