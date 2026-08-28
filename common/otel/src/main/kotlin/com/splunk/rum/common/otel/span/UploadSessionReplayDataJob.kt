/*
 * Copyright 2026 Splunk Inc.
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

package com.splunk.rum.common.otel.span

/**
 * Compatibility entry point for Session Replay jobs scheduled before the service moved from the
 * span package to the logRecord package.
 *
 * The old component name remains available because JobScheduler may already be dispatching a
 * persisted job while offline data migration is canceling and rescheduling it.
 */
internal class UploadSessionReplayDataJob : com.splunk.rum.agent.common.otel.logRecord.UploadSessionReplayDataJob()
