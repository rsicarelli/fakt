// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/** Name of the synthetic common producer task. */
internal const val SYNTHETIC_TASK_NAME: String = "faktGenerateCommonMain"

/** The one source set the synthetic common producer owns (and emits). */
internal const val SYNTHETIC_OWNED_SOURCE_SET: String = "commonMain"

private const val MAIN_COMPILATION = "main"
private const val JVM_TYPE = "jvm"

/**
 * Registers the synthetic common producer for KMP projects whose targets are all JVM-typed
 * (`jvm("desktop")` + `jvm("server")`, or AGP with `android.kmp.use.jvm.platform.type=true`). KGP
 * builds no `commonMain` metadata compilation for them, so without this task `commonTest` would
 * never see the `commonMain` fakes.
 *
 * The decision is made from the target list alone (no `afterEvaluate`, no other project is read),
 * so it is safe under Gradle Project Isolation. KGP resolves its subplugins after the `kotlin { }`
 * block has run, so the target set is complete when [registerIfRepresentative] is called.
 */
internal object SyntheticProducerWiring {

    /**
     * Registers the task when [compilation] is the `main` compilation of the representative target
     * that owns `commonMain` and that target is JVM-typed; otherwise does nothing. A task already
     * named [SYNTHETIC_TASK_NAME] that Fakt did not register itself is a name clash and fails the
     * build with a clear message instead of being skipped.
     */
    fun registerIfRepresentative(
        project: Project,
        compilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) {
        val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        val representative =
            kmp?.let { predictSyntheticCommonMainTarget(readSourceSetGraph(it).targets) }
        val isRepresentative =
            compilation.name == MAIN_COMPILATION &&
                representative?.name == compilation.target.targetName &&
                representative.platformType == JVM_TYPE
        if (!isRepresentative) return
        val existing = project.tasks.findByName(SYNTHETIC_TASK_NAME)
        when {
            existing == null ->
                FaktGenerateTaskWiring.registerSyntheticProducer(project, compilation, extension)
            isFaktTestDirOwner(project, existing.name) -> Unit
            else ->
                throw GradleException(
                    "Fakt needs the task name '$SYNTHETIC_TASK_NAME' for the common producer of " +
                        "an all-JVM project, but a task with that name already exists in " +
                        "project '${project.path}'. Rename that task."
                )
        }
    }
}

/**
 * The target that would own `commonMain` synthetically, or `null` when [targets] do not call for
 * one. This mirrors KGP: no `commonMain` metadata compilation exists when two or more real targets
 * share one non-native platform type. `commonMain` needs no `dependsOn` edges, every main source
 * set reaches it.
 */
internal fun predictSyntheticCommonMainTarget(targets: List<TargetNode>): TargetNode? {
    val parents = targets.flatMap { it.mainSourceSets }.associateWith { setOf("commonMain") }
    val owner = assignSourceSetOwners(SourceSetGraph(targets, parents))["commonMain"]
    return (owner as? SourceSetOwner.Synthetic)?.let { synthetic ->
        targets.first { it.name == synthetic.target }
    }
}

/**
 * `enabled`, and for the synthetic shapes also [syntheticOwnerConfirmed], so a project where KGP
 * did build a metadata compilation (a producer owns it) never emits the same set twice. The
 * confirmation is read when the task inputs resolve (never at registration, the compilations may
 * not exist yet) and remembered only once the project has finished evaluating.
 */
internal fun sourceGate(
    project: Project,
    shape: TaskShape,
    extension: FaktPluginExtension,
    compilation: KotlinCompilation<*>,
    owned: String?,
): Provider<Boolean> {
    val ownedSet =
        when (shape) {
            TaskShape.SYNTHETIC -> SYNTHETIC_OWNED_SOURCE_SET
            TaskShape.SYNTHETIC_INTERMEDIATE -> owned
            else -> null
        } ?: return extension.enabled
    var remembered: Boolean? = null
    val confirmed =
        project.provider {
            remembered
                ?: syntheticOwnerConfirmed(project, ownedSet, compilation.target.targetName).also {
                    if (project.state.executed) remembered = it
                }
        }
    return extension.enabled.zip(confirmed) { enabled, owns -> enabled && owns }
}

/**
 * Feeds the synthetic task: `commonMain` as emitted `commonSources`, and the representative's other
 * source sets as `platformAnalysisOnlySources` so its `actual`s pair with the common `expect`s.
 * `sources` stays empty, so with no `commonMain` sources the task is `NO-SOURCE`.
 */
internal fun configureSyntheticSources(
    task: FaktGenerateTask,
    representative: KotlinCompilation<*>,
    gate: Provider<Boolean>,
) {
    val commonMain =
        requireCommonMain(representative.allKotlinSourceSets, representative.target.targetName)
    val platform = representative.allKotlinSourceSets - commonMain
    task.commonSources.from(gate.gate(commonMain.kotlin))
    task.platformAnalysisOnlySources.from(gate.gate(platform.map { it.kotlin }))
}

/** The `commonMain` source set, or a clear error naming what the target does have. */
internal fun requireCommonMain(
    sourceSets: Collection<KotlinSourceSet>,
    targetName: String,
): KotlinSourceSet =
    sourceSets.firstOrNull { it.name == SYNTHETIC_OWNED_SOURCE_SET }
        ?: error(
            "Fakt could not find the '$SYNTHETIC_OWNED_SOURCE_SET' source set for target " +
                "'$targetName' (it has: ${sourceSets.map { it.name }}). The common producer of an " +
                "all-JVM project needs it."
        )

/**
 * Hands the task output to `commonTest` (lazy, so Gradle infers the task dependency), makes AGP
 * lint wait for it, and claims `commonTest` for it in the owner registry.
 */
internal fun wireSyntheticCommonTest(
    project: Project,
    taskProvider: TaskProvider<FaktGenerateTask>,
) {
    claimTestSourceSet(project, "commonTest", taskProvider.name)
    project.extensions
        .findByType(KotlinMultiplatformExtension::class.java)
        ?.sourceSets
        ?.findByName("commonTest")
        ?.kotlin
        ?.srcDir(taskProvider.flatMap { it.generatedKotlinDir })
    wireAndroidLintOrdering(project, taskProvider)
}

/** Where a task is named and writes, relative to the build directory. */
internal data class TaskLayout(
    val taskName: String,
    val outputPath: String,
    val scratchPath: String,
)

/**
 * Names the task and its directories. A KMP `commonMain` producer, and the synthetic common
 * producer, write to the canonical `commonTest` directory so the in-process plugin riding a
 * non-drivable platform compilation (Native) finds the common fakes there and dedup-skips them (no
 * `Redeclaration` in shared test sets). Every other compilation keeps a per-compilation directory.
 */
internal fun taskLayoutFor(
    compilation: KotlinCompilation<*>,
    shape: TaskShape,
    owned: String? = null,
): TaskLayout {
    if (shape == TaskShape.SYNTHETIC_INTERMEDIATE) {
        return syntheticIntermediateLayout(requireNotNull(owned) { "an owned source set" })
    }
    val target =
        compilation.target.targetName.ifBlank { compilation.target.platformType.name.lowercase() }
    val name = compilation.name
    val common = shape == TaskShape.SYNTHETIC
    return TaskLayout(
        taskName = if (common) SYNTHETIC_TASK_NAME else taskNameFor(target, name),
        outputPath =
            if (common || compilation.defaultSourceSet.name == SYNTHETIC_OWNED_SOURCE_SET) {
                CANONICAL_COMMON_TEST_DIR
            } else {
                "generated/fakt/$target/$name/kotlin"
            },
        scratchPath = if (common) "faktCaches/synthetic/commonMain" else "faktCaches/$target/$name",
    )
}
