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
    val data: Data
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

@Serializable
@SerialName("data")
internal data class Data(
    @SerialName("android:scheme")
    val scheme: String,
    @SerialName("android:host")
    val host: String,
    @SerialName("android:pathPattern")
    val pathPattern: String?,
    @SerialName("android:fragment")
    val fragment: String
)
