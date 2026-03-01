/*
 * Copyright 2026 DeepMatch Contributors
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

package com.aouledissa.deepmatch.api

/**
 * Marker interface implemented by generated deeplink parameter classes.
 *
 * When a spec declares typed path, query, or fragment values the Gradle plugin
 * emits a data class that implements [DeeplinkParams]. Runtime handlers can rely
 * on this type to access strongly-typed values in a uniform way.
 */
interface DeeplinkParams
