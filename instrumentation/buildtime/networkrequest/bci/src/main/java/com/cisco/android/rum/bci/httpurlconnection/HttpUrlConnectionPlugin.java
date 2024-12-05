/*
 * Copyright 2024 Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cisco.android.rum.bci.httpurlconnection;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;

import com.cisco.android.rum.library.httpurlconnection.HttpUrlReplacements;

import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.build.AndroidDescriptor;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;

public class HttpUrlConnectionPlugin implements Plugin {

    private final AndroidDescriptor androidDescriptor;

    public HttpUrlConnectionPlugin(AndroidDescriptor androidDescriptor) {
        this.androidDescriptor = androidDescriptor;
    }

    @Override
    public DynamicType.Builder<?> apply(
            DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassFileLocator classFileLocator) {

        try {
            return builder.visit(
                    MemberSubstitution.relaxed()
                            .method(is(URLConnection.class.getDeclaredMethod("connect")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForConnect", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getContent")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForContent", URLConnection.class))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getContent", Class[].class)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForContent",
                                            URLConnection.class,
                                            Class[].class))
                            .method(is(URLConnection.class.getDeclaredMethod("getContentType")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForContentType", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getContentEncoding")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForContentEncoding", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getContentLength")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForContentLength", URLConnection.class))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getContentLengthLong")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForContentLengthLong", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getExpiration")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForExpiration", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getDate")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForDate", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getLastModified")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForLastModified", URLConnection.class))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getHeaderField", String.class)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHeaderField",
                                            URLConnection.class,
                                            String.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getHeaderFields")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHeaderFields", URLConnection.class))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getHeaderFieldInt",
                                                    String.class,
                                                    Integer.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHeaderFieldInt",
                                            URLConnection.class,
                                            String.class,
                                            Integer.TYPE))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getHeaderFieldLong", String.class, Long.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHeaderFieldLong",
                                            URLConnection.class,
                                            String.class,
                                            Long.TYPE))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getHeaderField", Integer.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHeaderField",
                                            URLConnection.class,
                                            Integer.TYPE))
                            .method(
                                    is(
                                            HttpURLConnection.class.getDeclaredMethod(
                                                    "getHeaderField", Integer.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHttpHeaderField",
                                            HttpURLConnection.class,
                                            Integer.TYPE))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getHeaderFieldKey", Integer.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHeaderFieldKey",
                                            URLConnection.class,
                                            Integer.TYPE))
                            .method(
                                    is(
                                            HttpURLConnection.class.getDeclaredMethod(
                                                    "getHeaderFieldKey", Integer.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHttpHeaderFieldKey",
                                            HttpURLConnection.class,
                                            Integer.TYPE))
                            .method(
                                    is(
                                            URLConnection.class.getDeclaredMethod(
                                                    "getHeaderFieldDate", String.class, Long.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHeaderFieldDate",
                                            URLConnection.class,
                                            String.class,
                                            Long.TYPE))
                            .method(
                                    is(
                                            HttpURLConnection.class.getDeclaredMethod(
                                                    "getHeaderFieldDate", String.class, Long.TYPE)))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForHttpHeaderFieldDate",
                                            HttpURLConnection.class,
                                            String.class,
                                            Long.TYPE))
                            .method(
                                    is(
                                            HttpURLConnection.class.getDeclaredMethod(
                                                    "getResponseCode")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForResponseCode", URLConnection.class))
                            .method(
                                    is(
                                            HttpURLConnection.class.getDeclaredMethod(
                                                    "getResponseMessage")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForResponseMessage", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getOutputStream")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForOutputStream", URLConnection.class))
                            .method(is(URLConnection.class.getDeclaredMethod("getInputStream")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForInputStream", URLConnection.class))
                            .method(is(HttpURLConnection.class.getDeclaredMethod("getErrorStream")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForErrorStream", HttpURLConnection.class))
                            .method(is(HttpURLConnection.class.getDeclaredMethod("disconnect")))
                            .replaceWith(
                                    HttpUrlReplacements.class.getDeclaredMethod(
                                            "replacementForDisconnect", HttpURLConnection.class))
                            .on(isMethod()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // No operation.
    }

    @Override
    public boolean matches(TypeDescription target) {
        if (androidDescriptor.getTypeScope(target) == AndroidDescriptor.TypeScope.EXTERNAL) {
            return false;
        }
        return true;
    }
}

