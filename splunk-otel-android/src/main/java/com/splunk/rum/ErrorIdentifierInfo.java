/*
 * Copyright Splunk Inc.
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

package com.splunk.rum;

import androidx.annotation.Nullable;

public class ErrorIdentifierInfo {
    @Nullable private final String applicationId;
    @Nullable private final String versionCode;
    @Nullable private final String customUUID;

    public ErrorIdentifierInfo(
            @Nullable String applicationId,
            @Nullable String versionCode,
            @Nullable String customUUID) {
        this.applicationId = applicationId;
        this.versionCode = versionCode;
        this.customUUID = customUUID;
    }

    @Nullable
    public String getApplicationId() {
        return applicationId;
    }

    @Nullable
    public String getVersionCode() {
        return versionCode;
    }

    @Nullable
    public String getCustomUUID() {
        return customUUID;
    }
}
