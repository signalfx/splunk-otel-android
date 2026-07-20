# Woven okhttp3.OkHttpClient bytecode references OkHttpSingletons static fields by name.
-keep class com.splunk.rum.instrumentation.okhttp3.auto.internal.OkHttpSingletons {
    public static okhttp3.Interceptor callbackContextInterceptor;
    public static okhttp3.Interceptor resendCountContextInterceptor;
    public static okhttp3.Interceptor connectionErrorInterceptor;
    public static okhttp3.Interceptor tracingInterceptor;
}

# ByteBuddy callback weave calls into these classes from app okhttp3.RealCall bytecode.
-keep class com.splunk.rum.instrumentation.okhttp3.auto.internal.OkHttpCallbackAdviceHelper { *; }
-keep class com.splunk.rum.instrumentation.okhttp3.auto.internal.TracingCallback { *; }
