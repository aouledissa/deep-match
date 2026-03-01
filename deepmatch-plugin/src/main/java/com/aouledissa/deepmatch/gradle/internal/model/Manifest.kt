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

package com.aouledissa.deepmatch.gradle.internal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("manifest")
internal data class AndroidManifest(
    @SerialName("xmlns:android")
    val nameSpace: String = "http://schemas.android.com/apk/res/android",
    @SerialName("xmlns:tools")
    val toolsNamespace: String = "http://schemas.android.com/tools",
    val application: AndroidApplication,
)

@Serializable
@SerialName("application")
internal data class AndroidApplication(
    val activity: List<AndroidActivity>
)

@Serializable
@SerialName("activity")
internal data class AndroidActivity(
    @SerialName("android:name")
    val name: String,
    @SerialName("android:exported")
    val exported: Boolean = true,
    @SerialName("tools:node")
    val mergeStrategy: String = "merge",
    val intentFilter: List<IntentFilter>
)

@Serializable
@SerialName("intent-filter")
internal data class IntentFilter(
    @SerialName("android:autoVerify")
    val autoVerify: Boolean?,
    val action: List<Action>,
    val category: List<Category>,
    val scheme: List<Scheme>,
    val hosts: List<Host>,
    val port: Port?,
    val exactPaths: List<ExactPath>,
    val prefixPaths: List<PrefixPath>,
    val patternPaths: List<PatternPath>,
    val advancedPatternPaths: List<AdvancedPatternPath>,
)

@Serializable
@SerialName("action")
internal data class Action(
    @SerialName("android:name")
    val name: String,
)

@Serializable
@SerialName("category")
internal data class Category(
    @SerialName("android:name")
    val name: String,
)

@SerialName("data")
@Serializable
internal data class Scheme(
    @SerialName("android:scheme")
    val name: String,
    @SerialName("tools:ignore")
    val ignore: String? = null
)

@Serializable
@SerialName("data")
internal data class Host(
    @SerialName("android:host")
    val name: String
)

internal sealed interface PathData

@Serializable
@SerialName("data")
internal data class ExactPath(
    @SerialName("android:path")
    val path: String
) : PathData

@Serializable
@SerialName("data")
internal data class PrefixPath(
    @SerialName("android:pathPrefix")
    val prefix: String
) : PathData

@Serializable
@SerialName("data")
internal data class PatternPath(
    @SerialName("android:pathPattern")
    val pattern: String
) : PathData

@Serializable
@SerialName("data")
internal data class AdvancedPatternPath(
    @SerialName("android:pathAdvancedPattern")
    val pattern: String,
    @SerialName("tools:targetApi")
    val targetApi: String = "31"
) : PathData

@Serializable
@SerialName("data")
internal data class Port(
    @SerialName("android:port")
    val number: String
)
