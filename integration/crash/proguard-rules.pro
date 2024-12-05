-keep public class com.cisco.android.rum.integration.crash.installer.CrashInstaller
-keep public class com.cisco.android.rum.integration.crash.configurer.CrashConfigurer
-keepclassmembers class com.cisco.android.rum.integration.crash.configurer.CrashConfigurer {
    public static boolean isCrashReportingEnabled;
}
-repackageclasses 'com.cisco.android.rum.integration.crash'
