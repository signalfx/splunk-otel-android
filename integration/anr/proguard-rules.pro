-keep public class com.splunk.rum.integration.anr.ANRInstaller
-keep public class com.splunk.rum.integration.anr.ANRConfigurer
-keepclassmembers class com.splunk.rum.integration.anr.ANRConfigurer {
    public static boolean isANRReportingEnabled;
}
-repackageclasses 'com.splunk.rum.integration.anr'