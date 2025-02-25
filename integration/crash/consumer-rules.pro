-keep public class com.splunk.rum.integration.crash.CrashInstaller
-keep public class com.splunk.rum.integration.crash.configurer.CrashConfigurer
-keepclassmembers class com.splunk.rum.integration.crash.configurer.CrashConfigurer {
    public static boolean isCrashReportingEnabled;
}