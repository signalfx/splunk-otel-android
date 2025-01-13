-keep public class com.splunk.rum.integration.anr.installer.ANRInstaller
-keep public class com.splunk.rum.integration.anr.configurer.ANRConfigurer
-keepclassmembers class com.splunk.rum.integration.anr.configurer.ANRConfigurer {
    public static boolean isANRReportingEnabled;
}
-repackageclasses 'com.splunk.rum.integration.anr'