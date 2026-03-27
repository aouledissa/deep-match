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

package com.aouledissa.deepmatch.gradle.internal.task

import com.aouledissa.deepmatch.gradle.LOG_TAG
import com.aouledissa.deepmatch.gradle.ManifestSyncViolation
import com.aouledissa.deepmatch.gradle.internal.deserializeMergedDeeplinkConfigs
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

internal abstract class WarnManifestOutOfSyncTask : DefaultTask() {

    @get:InputFile
    abstract val specFileProperty: RegularFileProperty

    @get:InputFiles
    abstract val additionalSpecsFilesProperty: ConfigurableFileCollection

    @get:InputFile
    abstract val mergedManifestProperty: RegularFileProperty

    @get:Input
    abstract val violationProperty: Property<ManifestSyncViolation>

    @TaskAction
    fun validate() {
        val specsFiles = buildList {
            add(specFileProperty.asFile.get())
            addAll(additionalSpecsFilesProperty.files.sortedBy { it.absolutePath })
        }

        val yamlSerializer = Yaml(configuration = YamlConfiguration(strictMode = false))
        val deeplinkConfigs = yamlSerializer.deserializeMergedDeeplinkConfigs(specsFiles)

        val expectedSignatures = deeplinkConfigs.flatMap { config ->
            val hosts = config.host.filter { it.isNotEmpty() }.takeIf { it.isNotEmpty() } ?: listOf("")
            config.scheme.flatMap { scheme ->
                hosts.map { host ->
                    IntentFilterSignature(activity = config.activity, scheme = scheme, host = host)
                }
            }
        }.toSet()

        val manifestFile = mergedManifestProperty.asFile.get()
        val actualSignatures = parseManifestSignatures(manifestFile)

        val missing = expectedSignatures - actualSignatures
        if (missing.isNotEmpty()) {
            val details = missing
                .sortedWith(compareBy({ it.activity }, { it.scheme }, { it.host }))
                .joinToString(separator = "\n") { sig ->
                    val hostPart = if (sig.host.isNotEmpty()) " | host: ${sig.host}" else ""
                    "  - activity: ${sig.activity} | scheme: ${sig.scheme}$hostPart"
                }
            val message =
                "$LOG_TAG 'generateManifestFiles' is disabled but the following deeplink intent filters " +
                    "appear to be missing from AndroidManifest.xml:\n$details"

            when (violationProperty.get()) {
                ManifestSyncViolation.WARN -> logger.warn(message)
                ManifestSyncViolation.FAIL -> throw GradleException(message)
            }
        }
    }

    private fun parseManifestSignatures(manifestFile: java.io.File): Set<IntentFilterSignature> {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
        val document = factory.newDocumentBuilder().parse(manifestFile)
        return document.getElementsByTagName("activity")
            .elements()
            .flatMap { activity ->
                val activityName = activity.getAttribute("android:name").takeIf { it.isNotBlank() } ?: return@flatMap emptyList()
                activity.getElementsByTagName("intent-filter")
                    .elements()
                    .flatMap { signaturesFromIntentFilter(activityName, it) }
            }
            .toSet()
    }

    private fun signaturesFromIntentFilter(activityName: String, intentFilter: Element): List<IntentFilterSignature> {
        val dataElements = intentFilter.getElementsByTagName("data").elements()
        val schemes = dataElements.mapNotNull { it.getAttribute("android:scheme").takeIf { s -> s.isNotBlank() } }
        val hosts = dataElements.mapNotNull { it.getAttribute("android:host").takeIf { h -> h.isNotBlank() } }

        if (schemes.isEmpty()) return emptyList()
        val hostsToUse = hosts.takeIf { it.isNotEmpty() } ?: listOf("")
        return schemes.flatMap { scheme -> hostsToUse.map { host -> IntentFilterSignature(activityName, scheme, host) } }
    }

    private fun NodeList.elements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private data class IntentFilterSignature(
        val activity: String,
        val scheme: String,
        val host: String
    )
}
