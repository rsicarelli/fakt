// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.android.AndroidIntegration
import com.rsicarelli.fakt.gradle.android.AndroidVariantSources
import java.io.File
import java.util.Locale
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

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
    ) = register(project, kotlinCompilation, extension, TaskShape.PRODUCER)

    /**
     * Registers a consumer `FaktGenerateTask` for a drivable platform main (`jvmMain`,
     * `androidMain`, `jsMain`, `wasmJsMain`, …). It emits fakes ONLY for the compilation's own
     * source set ([KotlinCompilation.defaultSourceSet]); ancestor sources (commonMain and
     * intermediates) ride along as `analysisOnlySources` so the frontend can pair `actual`
     * declarations with their `expect`s and resolve common types in platform `@Fake` signatures —
     * their own fakes stay owned by the common producer (the context's route map restricts
     * emission), so nothing is emitted twice and the task stays fully cache-correct.
     */
    fun registerConsumer(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) = register(project, kotlinCompilation, extension, TaskShape.CONSUMER)

    /**
     * Registers the single `FaktGenerateTask` of a single-target KMP project (issue #153). With no
     * per-source-set `commonMain` metadata compilation to act as the common producer, the lone
     * platform main owns both halves in one compiler run: its own source set's fakes go to
     * `generatedKotlinDir` (wired into the platform test source set, like a consumer), and the
     * common fragment's fakes go to `commonGeneratedKotlinDir` — the canonical
     * `generated/fakt/commonTest/kotlin` — wired into `commonTest`. Owning the canonical directory
     * means the worker's reset also removes common fakes an earlier in-process build left there.
     */
    fun registerSingleTarget(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) = register(project, kotlinCompilation, extension, TaskShape.SINGLE_TARGET)

    /**
     * Registers the synthetic common producer (issue #160): when every KMP target is JVM-typed KGP
     * builds no `commonMain` metadata compilation, so one task, driven by the representative
     * target's [kotlinCompilation], owns the `commonMain` fakes. See [SyntheticProducerWiring].
     */
    fun registerSyntheticProducer(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) = register(project, kotlinCompilation, extension, TaskShape.SYNTHETIC)

    internal fun register(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
        shape: TaskShape,
        owned: String? = null,
    ) {
        val layout = taskLayoutFor(kotlinCompilation, shape, owned)
        val taskName = layout.taskName
        val compilationName = kotlinCompilation.name
        if (project.tasks.findByName(taskName) != null) return

        // applyToCompilation fires before afterEvaluate, so the configurations may not exist yet
        // by the time we land here. Idempotent helper makes the call safe to repeat.
        getSubpluginInstance(project).ensureFaktConfigurations(project)

        val outputDir = project.layout.buildDirectory.dir(layout.outputPath)
        val scratchDir = project.layout.buildDirectory.dir(layout.scratchPath)
        // Read when the input resolves, not now: the route map names source sets, and KGP's
        // `dependsOn` edges are not final while the compilation is still being applied.
        val placeholderJson =
            project.provider { encodePlaceholderContext(kotlinCompilation, shape, owned) }
        val workerClasspath = project.configurations.named(FaktGradleSubplugin.WORKER_CONFIGURATION)
        val compilerClasspath =
            project.configurations.named(FaktGradleSubplugin.COMPILER_CLASSPATH_CONFIGURATION)
        val commonOutputDir = project.layout.buildDirectory.dir(CANONICAL_COMMON_TEST_DIR)

        val taskProvider =
            project.tasks.register(taskName, FaktGenerateTask::class.java) { task ->
                configureSources(
                    task,
                    kotlinCompilation,
                    shape,
                    sourceGate(project, shape, extension, kotlinCompilation, owned),
                    owned,
                )
                if (shape == TaskShape.SINGLE_TARGET) {
                    task.commonGeneratedKotlinDir.set(commonOutputDir)
                }
                configureSourceSetRoots(task, kotlinCompilation)
                configureDependencies(task, kotlinCompilation)
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
                configureCompilerOptions(project, task, kotlinCompilation)
            }

        // AGP 9 built-in Kotlin: the compilation's Kotlin source sets are empty; the variant API
        // supplies the sources (issue #154).
        if (shape == TaskShape.PRODUCER && AndroidVariantSources.usesBuiltInKotlin(project)) {
            AndroidVariantSources.feed(project, compilationName, taskProvider)
        }
        when (shape) {
            TaskShape.SYNTHETIC -> wireSyntheticCommonTest(project, taskProvider)
            TaskShape.INTERMEDIATE_METADATA ->
                wireIntermediateTestDirs(project, kotlinCompilation, taskProvider)
            TaskShape.SYNTHETIC_INTERMEDIATE ->
                wireIntermediateTestDirs(project, kotlinCompilation, taskProvider, owned)
            else -> wireGeneratedDirConsumers(project, kotlinCompilation, extension, taskProvider)
        }
        if (shape == TaskShape.SINGLE_TARGET) {
            wireSingleTargetCommonDir(project, taskProvider)
        }
    }

    /**
     * Points everything that reads the task's `@OutputDirectory` at the task that fills it: the
     * matching test source set, the and AGP's lint analysis tasks.
     */
    private fun wireGeneratedDirConsumers(
        project: Project,
        kotlinCompilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
        taskProvider: TaskProvider<FaktGenerateTask>,
    ) {
        // Resolved once: honours `useGradleTestFixtures` AND the `java-test-fixtures` plugin being
        // applied (warns and falls back to `test` otherwise), matching the legacy path's semantics.
        val useTestFixtures =
            getSubpluginInstance(project).resolveTestFixturesMode(project, extension)
        wireTestSrcDirByAssociation(project, kotlinCompilation, taskProvider, useTestFixtures)
        wireAndroidLintOrdering(project, taskProvider)
    }

    /**
     * Sequences a non-drivable platform compilation (Native) after the common producer so the
     * producer's common fakes exist on disk before the in-process plugin's file-existence dedup
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

    /** Locate the [FaktGradleSubplugin] instance applied to [project] to call its helpers. */
    private fun getSubpluginInstance(project: Project): FaktGradleSubplugin =
        project.plugins.getPlugin(FaktGradleSubplugin::class.java)

    private fun encodePlaceholderContext(
        kotlinCompilation: KotlinCompilation<*>,
        shape: TaskShape,
        owned: String?,
    ): String {
        // `useTestFixtures` is intentionally left at its default here. In `buildContext` it only
        // affects `outputDirectory`, which the `.copy` below replaces with a placeholder and the
        // worker later overwrites with the task's real `generatedKotlinDir`. It changes nothing
        // else in the context, so test-fixtures routing is decided purely by which compile task
        // sources the generated dir (see `wireTestSrcDirByAssociation`), never by this serialized
        // JSON.
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
                .let {
                    val platformOwned = platformOwnedAncestorsOf(kotlinCompilation, shape)
                    it.copy(
                        allSourceSets =
                            appendMissingSourceSets(
                                it.allSourceSets,
                                kotlinCompilation.sourceSetInfos(),
                            ),
                        outputDirectories = outputRouteTokens(it, shape, owned, platformOwned),
                    )
                }
        val json = Json { prettyPrint = false }
        return json.encodeToString(SourceSetContext.serializer(), context)
    }
}

/**
 * Makes AGP's lint tasks depend on the generator that produces the sources they analyse.
 *
 * The generated directory reaches a Kotlin compilation through a `builtBy`-carrying
 * `FileCollection`, which is enough for `compileKotlin*`. AGP's lint tasks read the same directory
 * through the Android variant's own source model, which does **not** carry that task dependency, so
 * `lintAnalyze*` sees an input inside `FaktGenerateTask`'s `@OutputDirectory` with no path to it
 * and Gradle fails the build:
 * ```
 * Task ':lintAnalyzeAndroidHostTest' uses this output of task
 * ':faktGenerateMetadataCommonMain' without declaring an explicit or implicit dependency.
 * ```
 *
 * The failure only surfaces when the generator is actually in the task graph — `lint` alone leaves
 * it out (and then lints a directory nothing produced), while `build lint` pulls it in via the
 * compile tasks. Declaring the dependency here fixes both: lint now waits for the fakes, so it also
 * stops analysing a phantom directory.
 *
 * Only the cache-correct path needs this. The legacy in-process path writes the fakes as a side
 * effect of `compileKotlin*` with no declared output, so Gradle has nothing to validate.
 *
 * Safe under Gradle 9 Project Isolation: no `afterEvaluate` and no task-graph reads — the match is
 * lazy and `configureEach` only runs for lint tasks that are actually realized.
 */
internal fun wireAndroidLintOrdering(
    project: Project,
    taskProvider: TaskProvider<FaktGenerateTask>,
) {
    project.tasks
        .matching { isAgpLintAnalysisTask(it.name) }
        .configureEach { lintTask -> lintTask.dependsOn(taskProvider) }
}

/**
 * Whether [taskName] is one of AGP's lint *analysis* tasks (`lintAnalyze<Variant>`,
 * `lintVitalAnalyze<Variant>`, `lintAnalyzeAndroidHostTest`, …) — the tasks that read the generated
 * directory through the Android variant model. A bare `startsWith("lint")` also caught unrelated
 * tasks such as kotlinter's `lintKotlin`, which never touch the fakes. AGP's lint model writers
 * (`generate<Variant>LintModel`, `generate<Variant>UnitTestLintModel`, `…LintReportModel`,
 * `…LintVitalReportModel`) read the same source directories, so they are matched too.
 */
internal fun isAgpLintAnalysisTask(taskName: String): Boolean =
    taskName.startsWith("lintAnalyze") ||
        taskName.startsWith("lintVitalAnalyze") ||
        (taskName.startsWith("generate") &&
            (taskName.endsWith("LintModel") ||
                taskName.endsWith("LintReportModel") ||
                taskName.endsWith("LintVitalReportModel")))

/** Build-dir-relative canonical `commonTest` output, shared by the common producers. */
internal const val CANONICAL_COMMON_TEST_DIR: String = "generated/fakt/commonTest/kotlin"

/**
 * How a `FaktGenerateTask` partitions its compilation's sources (see the `register*` functions of
 * [FaktGenerateTaskWiring]).
 */
internal enum class TaskShape {
    /** Every source set analysed and emitted (KMP `commonMain`, single-platform JVM `main`). */
    PRODUCER,

    /** Own source set emitted; ancestors analysis-only (drivable KMP platform mains). */
    CONSUMER,

    /** Own source set and ancestors both emitted, each into its own output (single-target KMP). */
    SINGLE_TARGET,

    /** Only `commonMain` emitted; the representative platform is analysed (all-JVM KMP). */
    SYNTHETIC,

    /**
     * A JVM-only intermediate shared source set (`desktopAndServerMain`) owned by its
     * representative target: the owned set is emitted, `commonMain` and the other ancestors are
     * analysed (see [SyntheticIntermediateWiring]).
     */
    SYNTHETIC_INTERMEDIATE,

    /**
     * An intermediate shared source set (`webMain`) analysed by the metadata driver from its own
     * sources alone; the ancestors it refines arrive as klibs (see [IntermediateProducerWiring]).
     */
    INTERMEDIATE_METADATA,
}

/**
 * Feeds the compilation's source sets to [task] according to [shape]. The ancestors of a consumer's
 * or single-target task's default source set (commonMain and intermediates) are the common fragment
 * (`-Xcommon-sources`): analysis-only for a consumer — the common producer owns their fakes — and
 * emitted for a single-target task, which has no common producer.
 */
private fun configureSources(
    task: FaktGenerateTask,
    kotlinCompilation: KotlinCompilation<*>,
    shape: TaskShape,
    enabled: Provider<Boolean>,
    owned: String?,
) {
    val ancestors = kotlinCompilation.allKotlinSourceSets - kotlinCompilation.defaultSourceSet
    val own = kotlinCompilation.defaultSourceSet.kotlin
    when (shape) {
        TaskShape.PRODUCER ->
            task.sources.from(enabled.gate(kotlinCompilation.allKotlinSourceSets.map { it.kotlin }))
        TaskShape.CONSUMER -> {
            task.sources.from(kotlinCompilation.lazySources(enabled) { memberSplit(it).own })
            task.analysisOnlySources.from(
                kotlinCompilation.lazySources(enabled) { memberSplit(it).analysisOnly }
            )
        }
        TaskShape.SINGLE_TARGET -> {
            task.sources.from(enabled.gate(own))
            task.commonSources.from(enabled.gate(ancestors.map { it.kotlin }))
        }
        TaskShape.SYNTHETIC -> configureSyntheticSources(task, kotlinCompilation, enabled)
        TaskShape.INTERMEDIATE_METADATA ->
            configureIntermediateSources(task, kotlinCompilation, enabled)
        TaskShape.SYNTHETIC_INTERMEDIATE ->
            configureSyntheticIntermediateSources(task, kotlinCompilation, enabled, owned)
    }
}

/**
 * Yields [files] while Fakt is enabled and nothing otherwise. Feeding the task no sources makes
 * `@SkipWhenEmpty` report `NO-SOURCE`, and Gradle then deletes the stale outputs — which `onlyIf`
 * would leave in place.
 */
internal fun Provider<Boolean>.gate(files: Any): Provider<Any> = map { isEnabled ->
    if (isEnabled) files else emptyList<File>()
}

/**
 * Wires a single-target task's `commonGeneratedKotlinDir` into `commonTest` (lazy, so Gradle infers
 * the task dependency) and claims `commonTest` for the task in the owner registry.
 */
private fun wireSingleTargetCommonDir(
    project: Project,
    taskProvider: TaskProvider<FaktGenerateTask>,
) {
    claimTestSourceSet(project, "commonTest", taskProvider.name)
    project.extensions
        .findByType(KotlinMultiplatformExtension::class.java)
        ?.sourceSets
        ?.findByName("commonTest")
        ?.kotlin
        ?.srcDir(taskProvider.flatMap { it.commonGeneratedKotlinDir })
}

/**
 * Routes a compilation's dependencies to the input its compiler driver reads. Klib-based
 * compilations — common producers (`KotlinMetadataCompiler`, metadata klibs) and JS/Wasm platform
 * mains (`K2JSCompiler`, platform klibs) — go through the `@Classpath` `commonKlibClasspath`
 * (content-hashed; `@CompileClasspath` can fingerprint klibs as empty). JVM/Android compilations
 * feed their jar dependencies to the K2JVM driver through `compileClasspath`, Android ones plus the
 * SDK boot classpath.
 */
internal fun configureDependencies(
    task: FaktGenerateTask,
    kotlinCompilation: KotlinCompilation<*>,
) {
    val platformType = kotlinCompilation.target.platformType.name.lowercase()
    val isKlibBased =
        kotlinCompilation.defaultSourceSet.name == "commonMain" ||
            platformType in setOf("common", "js", "wasm")
    if (isKlibBased) {
        task.commonKlibClasspath.from(kotlinCompilation.compileDependencyFiles)
    } else {
        task.compileClasspath.from(kotlinCompilation.workerDependencyFiles())
    }
    // `-Xwasm-target` is read from KGP's own target model, so a custom target name
    // (`wasmJs("web")`) still resolves to the right flavour. Absent for Kotlin/JS.
    wasmCompilerTarget((kotlinCompilation.target as? KotlinJsIrTarget)?.wasmTargetType)
        ?.let(task.wasmTarget::set)
    val target = kotlinCompilation.target
    val needsBootClasspath =
        AndroidIntegration.needsBootClasspath(platformType) {
            AndroidIntegration.isKmpAndroidTarget(target)
        }
    if (needsBootClasspath) AndroidIntegration.addBootClasspath(task, kotlinCompilation.project)
}

internal fun taskNameFor(targetName: String, compilationName: String): String =
    "faktGenerate" + capitalizeAscii(targetName) + capitalizeAscii(compilationName)

private fun capitalizeAscii(s: String): String =
    if (s.isEmpty()) s else s.substring(0, 1).uppercase(Locale.ROOT) + s.substring(1)

/**
 * Maps KGP's [KotlinWasmTargetType] to the compiler's `-Xwasm-target` value; `null` (a Kotlin/JS
 * target) maps to `null`, leaving the `K2JSCompiler` driver in JS mode.
 */
internal fun wasmCompilerTarget(type: KotlinWasmTargetType?): String? =
    when (type) {
        KotlinWasmTargetType.JS -> "wasm-js"
        KotlinWasmTargetType.WASI -> "wasm-wasi"
        null -> null
    }
