package com.aouledissa.deepmatch.gradle.internal

import java.util.Locale

internal fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }