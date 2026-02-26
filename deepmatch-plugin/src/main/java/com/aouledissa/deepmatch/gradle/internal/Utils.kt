package com.aouledissa.deepmatch.gradle.internal

import com.aouledissa.deepmatch.gradle.internal.model.DeeplinkConfig
import com.aouledissa.deepmatch.gradle.internal.model.Specs
import com.charleskorn.kaml.Yaml
import java.io.File
import java.util.Locale

/**
 * Capitalizes the first character of the string.
 * If the first character is already uppercase, the string is returned unchanged.
 *
 * @receiver The string to capitalize.
 * @return The string with its first character capitalized.
 */
internal fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

/**
 * Converts a string to camelCase.
 *
 * Delimiters are whitespace, underscores, and hyphens.
 *
 * Examples:
 * - "foo bar" becomes "fooBar"
 * - "Foo-Bar" becomes "fooBar"
 * - "foo_bar" becomes "fooBar"
 * - "FOO_BAR" becomes "fooBar"
 * - "foo" remains "foo"
 * - "" remains ""
 *
 * @return The camelCase version of the string.
 */
internal fun String.toCamelCase(): String {
    if (this.isEmpty()) {
        return ""
    }
    val delimiters = Regex("\\s+|[_-]")
    if (!this.contains(delimiters)) {
        return this
    }
    return this.split(delimiters)
        .mapIndexed { index, string ->
            when {
                index == 0 -> string.lowercase()
                // Ensure the string is not empty before calling replaceFirstChar
                string.isNotEmpty() -> string.replaceFirstChar { it.uppercase() }
                else -> "" // Handle empty strings resulting from multiple delimiters
            }
        }.joinToString("")
}

internal fun generatedModuleSealedInterfaceName(moduleName: String): String {
    val normalized = moduleName
        .replace(Regex("[^A-Za-z0-9_\\-\\s]"), " ")
        .toCamelCase()
        .ifBlank { "module" }
    val typeSafeName = when {
        normalized.first().isDigit() -> "module${normalized.capitalize()}"
        else -> normalized
    }
    return "${typeSafeName.capitalize()}DeeplinkParams"
}

internal fun generatedModuleProcessorName(moduleName: String): String {
    val prefix = generatedModuleSealedInterfaceName(moduleName)
        .removeSuffix("DeeplinkParams")
    return "${prefix}DeeplinkProcessor"
}

internal fun Yaml.deserializeDeeplinkConfigs(file: File): List<DeeplinkConfig> {
    val content = file.readText()
    return decodeFromString(Specs.serializer(), content).deeplinkSpecs
}
