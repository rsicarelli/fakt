// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import java.util.Locale
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

/**
 * Registers a `FaktGenerateTask` for a single Kotlin compilation and wires its `@OutputDirectory`
 * into the matching test source set.
 *
 * Cross-classloader contract: every method here is plain Gradle API + the Fakt
 * [com.rsicarelli.fakt.compiler.api.SourceSetContext] data class. Nothing in
 * `kotlin-compiler-embeddable` is referenced.
 */
internal object FaktGenerateTaskWiring {

    /** Sentinel used inside the task's `@Input` JSON; the worker overwrites with real paths. */
    private const val OUTPUT_PLACEHOLDER: String = "fakt://generated"

    /** Build-dir token in the placeholder context, kept stable across machines for cache parity. */
    private const val BUILD_DIR_PLACEHOLDER: String = "<task-output>"

    /**
     * Registers a `FaktGenerateTask` for [kotlinCompilation], wires its output into the matching
     * test source set, and sets up the KMP cross-target `dependsOn` chain.
     */
    fun register(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) {
        val targetName =
            kotlinCompilation.target.targetName.ifBlank {
                kotlinCompilation.target.platformType.name.lowercase()
            }
        val compilationName = kotlinCompilation.name
        val taskName = taskNameFor(targetName, compilationName)
        if (project.tasks.findByName(taskName) != null) return

        // applyToCompilation fires before afterEvaluate, so the configurations may not exist yet
        // by the time we land here. Idempotent helper makes the call safe to repeat.
        getSubpluginInstance(project).ensureFaktConfigurations(project)

        val outputDir =
            project.layout.buildDirectory.dir("generated/fakt/$targetName/$compilationName/kotlin")
        val scratchDir =
            project.layout.buildDirectory.dir("faktCaches/$targetName/$compilationName")
        val firMetadataFile =
            project.layout.buildDirectory.file(
                "generated/fakt/$targetName/$compilationName/metadata/fir-metadata.json"
            )
        val placeholderJson = encodePlaceholderContext(kotlinCompilation)
        val workerClasspath = project.configurations.named(FaktGradleSubplugin.WORKER_CONFIGURATION)
        val compilerClasspath =
            project.configurations.named(FaktGradleSubplugin.COMPILER_CLASSPATH_CONFIGURATION)

        val taskProvider =
            project.tasks.register(taskName, FaktGenerateTask::class.java) { task ->
                task.sources.from(
                    kotlinCompilation.allKotlinSourceSets.map { sourceSet -> sourceSet.kotlin }
                )
                task.compileClasspath.from(commonProducerClasspath(project, kotlinCompilation))
                task.faktWorkerClasspath.from(workerClasspath)
                task.faktCompilerClasspath.from(compilerClasspath)
                task.sourceSetContextJson.set(placeholderJson)
                task.faktVersion.set(FaktGradleSubplugin.PLUGIN_VERSION)
                task.logLevel.set(extension.logLevel)
                task.imports.set(emptyList())
                task.generatedKotlinDir.set(outputDir)
                task.scratchDir.set(scratchDir)
                if (isMetadataLikeCompilation(kotlinCompilation)) {
                    task.firMetadataFile.set(firMetadataFile)
                }
            }

        wireTestSrcDir(project, kotlinCompilation, taskProvider)
        wireKmpDependsOnCommonMain(project, kotlinCompilation, taskProvider)
    }

    /**
     * Adds the task's `generatedKotlinDir` to the matching test source set as a Kotlin srcDir. Lazy
     * via `TaskProvider` so Gradle infers `builtBy` and downstream `compileKotlin*Test` waits for
     * the generator with no explicit `dependsOn`.
     */
    private fun wireTestSrcDir(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        taskProvider: TaskProvider<FaktGenerateTask>,
    ) {
        val testSourceSetName = mapMainToTest(kotlinCompilation.defaultSourceSet.name)
        val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        val generatedDirProvider = taskProvider.flatMap { it.generatedKotlinDir }
        if (kmp != null) {
            kmp.sourceSets.findByName(testSourceSetName)?.kotlin?.srcDir(generatedDirProvider)
        } else {
            project.tasks.withType(AbstractKotlinCompile::class.java).configureEach { compileTask ->
                if (compileTask.name.lowercase().contains("test")) {
                    compileTask.source(generatedDirProvider)
                }
            }
        }
    }

    /**
     * Every platform `faktGenerate*` task depends on the commonMain counterpart so common-source
     * `@Fake` declarations are validated once and reused. Mirrors KSP2's
     * `kspCommonMainKotlinMetadata` pattern; safe under Gradle 9 Project Isolation (no
     * `afterEvaluate`, no direct task-graph reads).
     */
    private fun wireKmpDependsOnCommonMain(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        taskProvider: TaskProvider<FaktGenerateTask>,
    ) {
        if (kotlinCompilation.defaultSourceSet.name == "commonMain") return
        if (project.extensions.findByType(KotlinMultiplatformExtension::class.java) == null) return
        taskProvider.configure { task ->
            val commonTask =
                project.tasks.findByName("faktGenerateMetadataCommonMain")
                    ?: project.tasks.findByName("faktGenerateCommonMain")
            if (commonTask != null) task.dependsOn(commonTask)
        }
    }

    /**
     * Compile classpath for the worker. For a KMP `commonMain` producer the worker drives
     * `K2JVMCompiler`, which needs a JVM-resolvable classpath — the JVM (or Android) target's
     * `main` compile dependencies, which carry the JVM stdlib plus the JVM variant of every project
     * and external dependency. `commonMain`'s own `compileDependencyFiles` are common `.klib`s that
     * the JVM compiler can't read, so they are not used here. Every other compilation uses its own
     * dependencies unchanged.
     */
    private fun commonProducerClasspath(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
    ): Any {
        if (kotlinCompilation.defaultSourceSet.name == "commonMain") {
            val jvmMain =
                project.extensions
                    .findByType(KotlinMultiplatformExtension::class.java)
                    ?.targets
                    ?.firstOrNull { target ->
                        target.platformType.name.lowercase().let {
                            it == "jvm" || it == "androidjvm"
                        }
                    }
                    ?.compilations
                    ?.findByName("main")
            if (jvmMain != null) return jvmMain.compileDependencyFiles
        }
        return kotlinCompilation.compileDependencyFiles
    }

    /** Locate the [FaktGradleSubplugin] instance applied to [project] to call its helpers. */
    private fun getSubpluginInstance(project: Project): FaktGradleSubplugin =
        project.plugins.getPlugin(FaktGradleSubplugin::class.java)

    private fun isMetadataLikeCompilation(kotlinCompilation: KotlinCompilation<*>): Boolean =
        kotlinCompilation.defaultSourceSet.name == "commonMain" ||
            kotlinCompilation.target.platformType.name.equals("common", ignoreCase = true)

    private fun encodePlaceholderContext(kotlinCompilation: KotlinCompilation<*>): String {
        val context =
            SourceSetDiscovery.buildContext(
                    kotlinCompilation,
                    buildDir = BUILD_DIR_PLACEHOLDER,
                    useTestFixtures = false,
                )
                .copy(
                    outputDirectory = OUTPUT_PLACEHOLDER,
                    commonTestOutputDirectory = OUTPUT_PLACEHOLDER,
                    metadataOutputPath = null,
                    metadataCachePath = null,
                )
        val json = Json { prettyPrint = false }
        return json.encodeToString(SourceSetContext.serializer(), context)
    }

    private fun taskNameFor(targetName: String, compilationName: String): String =
        "faktGenerate" + capitalizeAscii(targetName) + capitalizeAscii(compilationName)

    private fun mapMainToTest(sourceSetName: String): String =
        when {
            sourceSetName.equals("main", ignoreCase = true) -> "test"
            sourceSetName.endsWith("Main", ignoreCase = true) ->
                sourceSetName.removeSuffix("Main") + "Test"
            else -> sourceSetName + "Test"
        }

    private fun capitalizeAscii(s: String): String =
        if (s.isEmpty()) s else s.substring(0, 1).uppercase(Locale.ROOT) + s.substring(1)
}
