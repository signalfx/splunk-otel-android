package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.assertEquals;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import okhttp3.Call;
import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class CustomHeadersRequestInterceptorTest {

    @Test
    void interceptorAddsHeaders() throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Token");
        headers.put("Header", "Value");
        CustomHeadersRequestInterceptor interceptor = new CustomHeadersRequestInterceptor(() -> headers);

        Headers initialHeaders = Headers.of("Connection", "keep-alive");
        Consumer<Request> requestValidator = request -> {
            assertEquals(3, request.headers().size());
            assertEquals("keep-alive", request.headers().get("Connection"));
            assertEquals("Token", request.headers().get("Authorization"));
            assertEquals("Value", request.headers().get("Header"));
        };

        interceptor.intercept(testChain(initialHeaders, requestValidator));
    }

    @Test
    void interceptorAddsNoHeaderWhenSupplierReturnsNull() throws IOException {
        CustomHeadersRequestInterceptor interceptor = new CustomHeadersRequestInterceptor(() -> null);

        Headers initialHeaders = Headers.of("Connection", "keep-alive");
        Consumer<Request> requestValidator = request -> {
            assertEquals(1, request.headers().size());
            assertEquals("keep-alive", request.headers().get("Connection"));
        };

        interceptor.intercept(testChain(initialHeaders, requestValidator));
    }


    @Test
    void interceptorAddsLatestHeadersFromSupplier() throws IOException {
        AtomicReference<Integer> changingValue = new AtomicReference<>(0);
        Supplier<Map<String, String>> headersSupplier = () -> {
            changingValue.getAndSet(changingValue.get() + 1);
            Map<String, String> headers = new HashMap<>();
            headers.put("Header", changingValue.toString());
            return headers;
        };

        CustomHeadersRequestInterceptor interceptor = new CustomHeadersRequestInterceptor(headersSupplier);

        interceptor.intercept(testChain(Headers.of(), request -> {
            assertEquals(1, request.headers().size());
            assertEquals("1", request.headers().get("Header"));
        }));

        interceptor.intercept(testChain(Headers.of(), request -> {
            assertEquals(1, request.headers().size());
            assertEquals("2", request.headers().get("Header"));
        }));

        interceptor.intercept(testChain(Headers.of(), request -> {
            assertEquals(1, request.headers().size());
            assertEquals("3", request.headers().get("Header"));
        }));
    }

    private Interceptor.Chain testChain(Headers initialHeaders, Consumer<Request> requestValidator) {
        return new Interceptor.Chain() {
            @NonNull
            @Override
            public Request request() {
                return new Request.Builder().url("https://splunk.rum/test").headers(initialHeaders).build();
            }

            @NonNull
            @Override
            public Response proceed(@NonNull Request request) {
                requestValidator.accept(request);
                return new Response.Builder().request(request).code(200).message("OK").protocol(Protocol.HTTP_2).build();
            }

            @Nullable
            @Override
            public Connection connection() {
                return null;
            }

            @NonNull
            @Override
            public Call call() {
                return null;
            }

            @Override
            public int connectTimeoutMillis() {
                return 0;
            }

            @NonNull
            @Override
            public Interceptor.Chain withConnectTimeout(int i, @NonNull TimeUnit timeUnit) {
                return this;
            }

            @Override
            public int readTimeoutMillis() {
                return 0;
            }

            @NonNull
            @Override
            public Interceptor.Chain withReadTimeout(int i, @NonNull TimeUnit timeUnit) {
                return this;
            }

            @Override
            public int writeTimeoutMillis() {
                return 0;
            }

            @NonNull
            @Override
            public Interceptor.Chain withWriteTimeout(int i, @NonNull TimeUnit timeUnit) {
                return this;
            }
        };
    }
}
