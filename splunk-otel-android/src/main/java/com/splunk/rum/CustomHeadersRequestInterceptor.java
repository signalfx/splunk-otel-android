package com.splunk.rum;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CustomHeadersRequestInterceptor implements Interceptor {

    @NonNull
    private final Supplier<Map<String, String>> headersSupplier;


    public CustomHeadersRequestInterceptor(@NonNull Supplier<Map<String, String>> headersSupplier) {
        this.headersSupplier = headersSupplier;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();
        Map<String, String> headers = headersSupplier.get();
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }
        return chain.proceed(requestBuilder.build());
    }
}
