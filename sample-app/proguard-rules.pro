# Ignore missing class warnings from dependencies
-dontwarn com.fasterxml.jackson.**
-dontwarn com.google.auto.value.**
-dontwarn com.google.errorprone.**
-dontwarn com.google.common.io.**
-dontwarn io.grpc.**
-dontwarn org.osgi.annotation.**

# Critical for deobfuscation - keeps line numbers and file names in stacktraces
-keepattributes SourceFile,LineNumberTable
# Changes source file names in stacktraces to "mySourceFile"
#-renamesourcefileattribute mySourceFile

# Preserve details needed for reflection, annotations and lambdas
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes MethodParameters,Exceptions
-keepattributes *Annotation*

# Force R8/ProGuard to keep package structure
-keeppackagenames

# Allows changing access modifiers to enable more optimizations
#-allowaccessmodification
# Uses same names for different methods when possible
#-overloadaggressively
