# Used via reflection by SplunkRum.createRumOkHttpCallFactory() legacy API.
# LegacyAPIReflectionUtils reads Companion and calls getInstance() on the companion object.
-keep class com.splunk.rum.integration.okhttp3.manual.OkHttpManualInstrumentation { *; }
-keep class com.splunk.rum.integration.okhttp3.manual.OkHttpManualInstrumentation$Companion { *; }
