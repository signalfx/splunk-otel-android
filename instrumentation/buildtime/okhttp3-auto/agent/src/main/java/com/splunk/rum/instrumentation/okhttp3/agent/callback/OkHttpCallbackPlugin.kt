/*
 * Copyright The OpenTelemetry Authors
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

package com.splunk.rum.instrumentation.okhttp3.agent.callback

import java.io.IOException
import java.util.regex.Pattern
import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import okhttp3.Callback

internal class OkHttpCallbackPlugin : Plugin {

    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> = builder.visit(
        Advice.to(OkHttpCallbackAdvice::class.java)
            .on(
                ElementMatchers.named<NamedElement>("enqueue").and(
                    ElementMatchers.takesArgument(
                        0,
                        Callback::class.java
                    )
                )
            )
    )

    @Throws(IOException::class)
    override fun close() {
        // No operation.
    }

    override fun matches(target: TypeDescription): Boolean = REAL_CALL_PATTERN.matcher(target.typeName).matches()

    companion object {
        private val REAL_CALL_PATTERN: Pattern = Pattern.compile("^okhttp3\\..*RealCall$")
    }
}
