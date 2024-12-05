-keep public class com.cisco.android.rum.integration.networkrequest.installer.NetworkRequestInstaller
-keep public class com.cisco.android.rum.integration.networkrequest.configurer.NetworkRequestConfigurer
-keepclassmembers class com.cisco.android.rum.integration.networkrequest.configurer.NetworkRequestConfigurer {
    public static boolean isNetworkTracingEnabled;
}