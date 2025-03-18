# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Ignore missing class warnings from dependencies
-dontwarn com.fasterxml.jackson.**
-dontwarn com.google.auto.value.**
-dontwarn com.google.errorprone.**
-dontwarn com.google.common.io.**
-dontwarn io.grpc.**
-dontwarn org.osgi.annotation.**

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve details needed for reflection, annotations and lambdas
#-keepattributes Signature,InnerClasses,EnclosingMethod
#-keepattributes MethodParameters,Exceptions
#-keepattributes *Annotation*

# Force R8/ProGuard to keep package structure
#-keeppackagenames

# Allows changing access modifiers to enable more optimizations
#-allowaccessmodification
# Uses same names for different methods when possible
#-overloadaggressively
