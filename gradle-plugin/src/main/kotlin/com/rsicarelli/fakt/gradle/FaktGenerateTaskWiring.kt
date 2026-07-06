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
     * Registers a producer `FaktGenerateTask`: it analyses the whole compilation
     * ([KotlinCompilation.allKotlinSourceSets], so a KMP `commonMain` producer also sees its
     * ancestors) and, for `commonMain`, owns the common fakes the rest of the build reuses.
     */
    fun registerProducer(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) = register(project, kotlinCompilation, extension, partitionToOwnSourceSet = false)

    /**
     * Registers a consumer `FaktGenerateTask` for a drivable platform main (`jvmMain` /
     * `androidMain`). It emits fakes ONLY for the compilation's own source set
     * ([KotlinCompilation.defaultSourceSet]); ancestor sources (commonMain and intermediates) ride
     * along as `analysisOnlySources` so the frontend can pair `actual` declarations with their
     * `expect`s and resolve common types in platform `@Fake` signatures — their own fakes stay
     * owned by the common producer (`SourceSetContext.emitSourceSets` restricts emission), so
     * nothing is emitted twice and the task stays fully cache-correct.
     */
    fun registerConsumer(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) = register(project, kotlinCompilation, extension, partitionToOwnSourceSet = true)

    private fun register(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
        partitionToOwnSourceSet: Boolean,
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

        // A KMP `commonMain` producer writes to the canonical `commonTest` directory so the
        // in-process plugin riding a non-drivable platform compilation (Native/JS/Wasm) finds the
        // common fakes there and dedup-skips them, generating only its own platform-specific fakes
        // —
        // no `Redeclaration` in shared test sets. Every other compilation keeps a per-compilation
        // directory.
        val generatedKotlinPath =
            if (kotlinCompilation.defaultSourceSet.name == "commonMain") {
                "generated/fakt/commonTest/kotlin"
            } else {
                "generated/fakt/$targetName/$compilationName/kotlin"
            }
        val outputDir = project.layout.buildDirectory.dir(generatedKotlinPath)
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
        val sourceSets =
            if (partitionToOwnSourceSet) listOf(kotlinCompilation.defaultSourceSet)
            else kotlinCompilation.allKotlinSourceSets.toList()

        val taskProvider =
            project.tasks.register(taskName, FaktGenerateTask::class.java) { task ->
                task.sources.from(sourceSets.map { sourceSet -> sourceSet.kotlin })
                if (partitionToOwnSourceSet) {
                    val ancestors =
                        kotlinCompilation.allKotlinSourceSets - kotlinCompilation.defaultSourceSet
                    task.analysisOnlySources.from(ancestors.map { sourceSet -> sourceSet.kotlin })
                }
                // Common producers drive KotlinMetadataCompiler, which reads the compilation's own
                // metadata-klib dependencies — routed through the @Classpath commonKlibClasspath
                // input (content-hashed; @CompileClasspath can fingerprint klibs as empty). Every
                // other compilation feeds its JVM dependencies to the K2JVM driver unchanged.
                if (isMetadataLikeCompilation(kotlinCompilation)) {
                    task.commonKlibClasspath.from(kotlinCompilation.compileDependencyFiles)
                    task.firMetadataFile.set(firMetadataFile)
                } else {
                    task.compileClasspath.from(kotlinCompilation.compileDependencyFiles)
                }
                task.faktWorkerClasspath.from(workerClasspath)
                task.faktCompilerClasspath.from(compilerClasspath)
                task.sourceSetContextJson.set(placeholderJson)
                task.faktVersion.set(FaktGradleSubplugin.PLUGIN_VERSION)
                task.logLevel.set(extension.logLevel)
                task.enableCallHistory.set(extension.enableCallHistory)
                task.enableMutableFakes.set(extension.enableMutableFakes)
                task.imports.set(emptyList())
                task.generatedKotlinDir.set(outputDir)
                task.scratchDir.set(scratchDir)
            }

        wireTestSrcDir(project, kotlinCompilation, taskProvider)
        wireKmpDependsOnCommonMain(project, kotlinCompilation, taskProvider)
    }

    /**
     * Sequences a non-drivable platform compilation (Native/JS/Wasm) after the common producer so
     * the producer's common fakes exist on disk before the in-process plugin's file-existence dedup
     * runs on the platform main compile — otherwise it would regenerate them and collide. The test
     * compile already waits for the producer transitively through the `commonTest` srcDir's
     * `builtBy`, so only the main compile is wired here. Safe under Gradle 9 Project Isolation (no
     * `afterEvaluate`, no task-graph reads); the producer lookup runs lazily inside
     * `configureEach`.
     */
    fun wireLegacyHybridOrdering(project: Project, kotlinCompilation: KotlinCompilation<*>) {
        if (project.extensions.findByType(KotlinMultiplatformExtension::class.java) == null) return
        val mainCompileTask = kotlinCompilation.compileKotlinTaskName
        project.tasks
            .matching { it.name == mainCompileTask }
            .configureEach { compileTask ->
                val commonTask =
                    project.tasks.findByName("faktGenerateMetadataCommonMain")
                        ?: project.tasks.findByName("faktGenerateCommonMain")
                if (commonTask != null) compileTask.dependsOn(commonTask)
            }
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
