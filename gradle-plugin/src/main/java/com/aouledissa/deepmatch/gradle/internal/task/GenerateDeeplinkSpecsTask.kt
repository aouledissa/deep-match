package com.aouledissa.deepmatch.gradle.internal.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

internal abstract class GenerateDeeplinkSpecsTask : DefaultTask() {

    @get:InputFile
    abstract val specsFileProperty: RegularFileProperty

    @get:InputFile
    abstract val packageNameProperty: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generateDeeplinkSpecs() {

    }
}