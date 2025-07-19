package com.aouledissa.deepmatch.gradle.internal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("manifest")
internal data class AndroidManifest(
    @SerialName("xmlns:android")
    val nameSpace: String = "http://schemas.android.com/apk/res/android",
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
    val intentFilter: List<IntentFilter>
)

@Serializable
@SerialName("intent-filter")
internal data class IntentFilter(
    @SerialName("android:autoVerify")
    val autoVerify: Boolean?,
    val action: List<Action>,
    val category: List<Category>,
    val scheme: Scheme,
    val hosts: List<Host>,
    val pathPattern: PathPattern?,
    val fragment: Fragment?
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
    val name: String
)

@Serializable
@SerialName("data")
internal data class Host(
    @SerialName("android:host")
    val name: String
)

@Serializable
@SerialName("data")
internal data class PathPattern(
    @SerialName("android:pathPattern")
    val pattern: String
)

@Serializable
@SerialName("data")
internal data class Fragment(
    @SerialName("android:fragment")
    val name: String
)
