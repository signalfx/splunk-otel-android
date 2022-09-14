import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway

plugins {
    id("net.ltgt.errorprone")
    id("net.ltgt.nullaway")
}

dependencies {
    errorprone("com.uber.nullaway:nullaway:0.9.9")
    errorprone("com.google.errorprone:error_prone_core:2.15.0")
    errorproneJavac("com.google.errorprone:javac:9+181-r4173-1")
}

nullaway {
    annotatedPackages.add("com.splunk.rum")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.errorprone {
            if (name.toLowerCase().contains("test")) {
                // just disable all error prone checks for test
                isEnabled.set(false);
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
