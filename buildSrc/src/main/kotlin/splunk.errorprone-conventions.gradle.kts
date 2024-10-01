import com.android.build.api.variant.AndroidComponentsExtension
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import java.util.Locale

plugins {
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
}


val isAndroidProject = extensions.findByName("android") != null

if (isAndroidProject) {
    val errorProneConfig = configurations.getByName(ErrorPronePlugin.CONFIGURATION_NAME)
    extensions.getByType(AndroidComponentsExtension::class.java).onVariants {
        it.annotationProcessorConfiguration.extendsFrom(errorProneConfig)
    }
}

dependencies {
    errorprone("com.uber.nullaway:nullaway:0.11.3")
    errorprone("com.google.errorprone:error_prone_core:2.33.0")
    errorproneJavac("com.google.errorprone:javac:9+181-r4173-1")
}

nullaway {
    annotatedPackages.add("io.opentelemetry.rum.internal")
    annotatedPackages.add("com.splunk.rum")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.errorprone {
            if (name.lowercase(Locale.getDefault()).contains("test")) {
                // just disable all error prone checks for test
                isEnabled.set(false);
                isCompilingTestOnlyCode.set(true)
            } else {
                if (isAndroidProject) {
                    isEnabled.set(true)
                    isCompilingTestOnlyCode.set(false)
                }
            }

            nullaway {
                severity.set(CheckSeverity.ERROR)
            }

            // Builder 'return this;' pattern
            disable("CanIgnoreReturnValueSuggester")
            // Common to avoid an allocation
            disable("MixedMutabilityReturnType")
        }
    }
}