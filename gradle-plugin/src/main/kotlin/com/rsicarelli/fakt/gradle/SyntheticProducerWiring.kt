// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.android.AndroidIntegration
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget

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
     * named [SYNTHETIC_TASK_NAME] that is not a `FaktGenerateTask` is a name clash and fails the
     * build with a clear message instead of being skipped.
     */
    fun registerIfRepresentative(
        project: Project,
        compilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) {
        val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        val representative = kmp?.let { predictSyntheticCommonMainTarget(targetNodes(it)) }
        val isRepresentative =
            compilation.name == MAIN_COMPILATION &&
                representative?.name == compilation.target.targetName &&
                representative.platformType == JVM_TYPE
        if (!isRepresentative) return
        when (project.tasks.findByName(SYNTHETIC_TASK_NAME)) {
            null ->
                FaktGenerateTaskWiring.registerSyntheticProducer(project, compilation, extension)
            is FaktGenerateTask -> Unit
            else ->
                throw GradleException(
                    "Fakt needs the task name '$SYNTHETIC_TASK_NAME' in project " +
                        "'${project.path}' to generate the commonMain fakes of an all-JVM " +
                        "Kotlin Multiplatform project, but a different task already uses it. " +
                        "Rename that task."
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

private fun targetNodes(kmp: KotlinMultiplatformExtension): List<TargetNode> =
    kmp.targets
        .filter { it !is KotlinMetadataTarget }
        .map { target ->
            TargetNode(
                name = target.targetName,
                platformType = target.platformType.name.lowercase(),
                isAndroid = target.isAndroidTarget(),
                mainSourceSets =
                    listOf(
                        target.compilations.findByName(MAIN_COMPILATION)?.defaultSourceSet?.name
                            ?: "${target.targetName}Main"
                    ),
            )
        }

private fun KotlinTarget.isAndroidTarget(): Boolean =
    platformType.name.equals("androidJvm", ignoreCase = true) ||
        AndroidIntegration.isKmpAndroidTarget(this)

/**
 * `enabled`, and for the synthetic shape also [ownsCommonMain], so a project where KGP did build a
 * `commonMain` metadata compilation (a producer owns it) never emits it twice.
 */
internal fun sourceGate(
    project: Project,
    shape: TaskShape,
    extension: FaktPluginExtension,
): Provider<Boolean> =
    if (shape == TaskShape.SYNTHETIC) {
        extension.enabled.zip(ownsCommonMain(project)) { enabled, owns -> enabled && owns }
    } else {
        extension.enabled
    }

/**
 * Safety net for the prediction: true while the metadata target has no `commonMain` compilation. It
 * is read when the task inputs resolve (never at registration, the compilations may not exist yet)
 * and remembered only once the project has finished evaluating.
 */
private fun ownsCommonMain(project: Project): Provider<Boolean> {
    var remembered: Boolean? = null
    return project.provider {
        remembered
            ?: project.extensions
                .findByType(KotlinMultiplatformExtension::class.java)
                ?.targets
                ?.withType(KotlinMetadataTarget::class.java)
                ?.firstOrNull()
                ?.compilations
                ?.findByName(SYNTHETIC_OWNED_SOURCE_SET)
                .let { it == null }
                .also { if (project.state.executed) remembered = it }
    }
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
        representative.allKotlinSourceSets.first { it.name == SYNTHETIC_OWNED_SOURCE_SET }
    val platform = representative.allKotlinSourceSets - commonMain
    task.commonSources.from(gate.gate(commonMain.kotlin))
    task.platformAnalysisOnlySources.from(gate.gate(platform.map { it.kotlin }))
}

/**
 * Hands the task output to `commonTest` (lazy, so Gradle infers the task dependency), makes AGP
 * lint wait for it, and records it as the owner of the canonical `commonTest` directory.
 */
internal fun wireSyntheticCommonTest(
    project: Project,
    taskProvider: TaskProvider<FaktGenerateTask>,
) {
    project.extensions.extraProperties.set(COMMON_TEST_OWNER_PROPERTY, taskProvider.name)
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
internal fun taskLayoutFor(compilation: KotlinCompilation<*>, shape: TaskShape): TaskLayout {
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
