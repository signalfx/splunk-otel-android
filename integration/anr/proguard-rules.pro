-keep public class com.cisco.android.rum.integration.anr.installer.ANRInstaller
-keep public class com.cisco.android.rum.integration.anr.configurer.ANRConfigurer
-keepclassmembers class com.cisco.android.rum.integration.anr.configurer.ANRConfigurer {
    public static boolean isANRReportingEnabled;
}
-repackageclasses 'com.cisco.android.rum.integration.anr'