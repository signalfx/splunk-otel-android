-keep public class com.splunk.rum.integration.networkrequest.NetworkRequestInstaller
-keep public class com.splunk.rum.integration.networkrequest.configurer.NetworkRequestConfigurer
-keepclassmembers class com.splunk.rum.integration.networkrequest.configurer.NetworkRequestConfigurer {
    public static boolean isNetworkTracingEnabled;
}