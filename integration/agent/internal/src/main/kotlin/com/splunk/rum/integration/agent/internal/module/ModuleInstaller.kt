/*
 * Copyright 2025 Splunk Inc.
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

package com.splunk.rum.integration.agent.internal.module

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

abstract class ModuleInstaller : ContentProvider() {

    protected abstract fun onInstall(context: Context)

    final override fun onCreate(): Boolean {
        val context = context ?: throw IllegalStateException("Property 'context' is null")
        onInstall(context)
        return true
    }

    final override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    final override fun getType(uri: Uri): String? = null

    final override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    final override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    final override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
