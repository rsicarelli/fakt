// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

private const val KMP_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
private const val MAIN_COMPILATION = "main"
private const val COMMON_MAIN = "commonMain"
private const val JVM_TYPE = "jvm"

/**
 * Registers a synthetic producer for every intermediate source set (`desktopAndServerMain`) that
 * several JVM-typed targets share. KGP builds no usable metadata compilation for it (it disables
 * the compile task of a set only JVM and Android JVM targets compile), so one task on the
 * representative target's `main` compilation owns the set's fakes: `faktGenerate<Set>`.
 *
 * `commonMain` keeps its own task ([SyntheticProducerWiring]); everything here is for the other
 * synthetic owners. Only a JVM representative is supported: a set shared by js-only or wasm-only
 * targets stays unsupported and is reported with a warning.
 *
 * Registration happens twice, because KGP finishes the `dependsOn` edges in stages. [registerEager]
 * runs from the representative's `applyToCompilation`, where explicit edges are already visible;
 * [registerMissing] runs from an `afterEvaluate` that [installLatePass] registers through
 * `pluginManager.withPlugin`, and picks up whatever the eager call could not see. Both are
 * idempotent. Edges that user code adds in a still later `afterEvaluate` stay a known limit.
 */
internal object SyntheticIntermediateWiring {

    /**
     * Registers the intermediate tasks [compilation] represents, when it is the `main` compilation
     * of a JVM target that owns synthetic intermediates. Does nothing for any other compilation.
     */
    fun registerEager(
        project: Project,
        compilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
    ) {
        val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (kmp == null || compilation.name != MAIN_COMPILATION) return
        val graph = readSourceSetGraph(kmp)
        val target = graph.targets.firstOrNull { it.name == compilation.target.targetName }
        if (target?.platformType == JVM_TYPE) {
            syntheticOwnersOf(assignSourceSetOwners(graph), target.name)
                .filter { it.sourceSet != COMMON_MAIN }
                .forEach { register(project, compilation, extension, it.sourceSet) }
        }
    }

    /**
     * Registers every confirmed synthetic intermediate owner that has no task yet, and warns about
     * the ones that end without one (a non-JVM representative, which is unsupported). A project
     * whose representative has no consumer task is not on the cache-correct path, so it is left
     * alone.
     */
    fun registerMissing(
        project: Project,
        extension: FaktPluginExtension,
        warn: (String) -> Unit = project.logger::warn,
    ) {
        val kmp = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return
        val graph = readSourceSetGraph(kmp)
        assignSourceSetOwners(graph)
            .values
            .filterIsInstance<SourceSetOwner.Synthetic>()
            .filter { it.sourceSet != COMMON_MAIN }
            .sortedBy { it.sourceSet }
            .forEach { owner ->
                val target = graph.targets.first { it.name == owner.target }
                val compilation =
                    kmp.targets.getByName(owner.target).compilations.findByName(MAIN_COMPILATION)
                if (compilation == null || !isOnTaskPath(project, compilation)) return@forEach
                if (target.platformType == JVM_TYPE) {
                    register(project, compilation, extension, owner.sourceSet)
                } else {
                    warn(unsupportedSyntheticOwnerMessage(owner, target.platformType))
                }
            }
    }

    /** Hooks the late pass onto the KMP plugin, so it runs after KGP's own `afterEvaluate`. */
    fun installLatePass(project: Project, extension: FaktPluginExtension) {
        project.pluginManager.withPlugin(KMP_PLUGIN_ID) {
            project.afterEvaluate { registerMissing(project, extension) }
        }
    }

    private fun register(
        project: Project,
        compilation: KotlinCompilation<*>,
        extension: FaktPluginExtension,
        sourceSet: String,
    ) {
        val shape = TaskShape.SYNTHETIC_INTERMEDIATE
        val taskName = taskLayoutFor(compilation, shape, sourceSet).taskName
        val existing = project.tasks.findByName(taskName)
        when {
            existing == null ->
                FaktGenerateTaskWiring.register(project, compilation, extension, shape, sourceSet)
            existing is FaktGenerateTask && isFaktTestDirOwner(project, taskName) -> Unit
            else ->
                throw GradleException(
                    "Fakt needs the task name '$taskName' for the producer of the '$sourceSet' " +
                        "source set, but a task with that name already exists in project " +
                        "'${project.path}'. Rename that task."
                )
        }
    }

    /** A consumer task for [compilation] exists, so the project generates through the worker. */
    private fun isOnTaskPath(project: Project, compilation: KotlinCompilation<*>): Boolean =
        (project.tasks.findByName(taskNameFor(compilation.target.targetName, compilation.name))
            is FaktGenerateTask)
}

/**
 * Whether [target] is, right now, the confirmed synthetic owner of [sourceSet]: the live graph,
 * with every metadata compilation KGP has built so far, still assigns the set to that
 * representative. A set KGP gave a metadata producer, or one that a platform or another target
 * owns, answers false, so a synthetic task never generates what another task owns.
 */
internal fun syntheticOwnerConfirmed(project: Project, sourceSet: String, target: String): Boolean {
    val kmp =
        project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return false
    val owner = assignSourceSetOwners(readSourceSetGraph(kmp))[sourceSet]
    return owner is SourceSetOwner.Synthetic && owner.target == target
}

/** Task name, output and scratch layout of the synthetic producer of [owned]. */
internal fun syntheticIntermediateLayout(owned: String): TaskLayout =
    TaskLayout(
        // The target part of the name is empty on purpose: the task is named after the set.
        taskName = taskNameFor("", owned),
        outputPath = "generated/fakt/synthetic/$owned/kotlin",
        scratchPath = "faktCaches/synthetic/$owned",
    )

/**
 * Feeds a synthetic intermediate task. The owned set is the only emitted input ([task] `sources`,
 * so an empty set makes the task `NO-SOURCE`); its ancestors (`commonMain` and any set above) are
 * analysis-only common-fragment sources, and every other source set of the representative is
 * analysed as platform code, so `actual`s pair with their `expect`s. The split is read at
 * registration, when the owner is confirmed and the `dependsOn` edges are visible.
 */
internal fun configureSyntheticIntermediateSources(
    task: FaktGenerateTask,
    representative: KotlinCompilation<*>,
    gate: Provider<Boolean>,
    owned: String?,
) {
    val sourceSet = requireNotNull(owned) { "an owned source set" }
    // Read now, not inside a lambda: a lambda would capture the compilation, which the
    // configuration cache cannot store. The synthetic task is registered once the owner is
    // confirmed, so the dependsOn edges are visible here.
    val split = SourceSplit.of(representative, sourceSet)
    task.sources.from(gate.gate(split.owned.kotlin))
    task.analysisOnlySources.from(gate.gate(split.ancestors.map { it.kotlin }))
    task.platformAnalysisOnlySources.from(gate.gate(split.platform.map { it.kotlin }))
}

private class SourceSplit(
    val owned: KotlinSourceSet,
    val ancestors: List<KotlinSourceSet>,
    val platform: List<KotlinSourceSet>,
) {
    companion object {
        fun of(representative: KotlinCompilation<*>, owned: String): SourceSplit {
            val all = representative.allKotlinSourceSets
            val ownedSet =
                all.firstOrNull { it.name == owned }
                    ?: error(
                        "Fakt could not find the '$owned' source set for target " +
                            "'${representative.target.targetName}' (it has: ${all.map { it.name }})."
                    )
            val ancestors = ownedSet.getAllParentSourceSets().filter { it != ownedSet }
            return SourceSplit(
                ownedSet,
                ancestors,
                all.filter { it != ownedSet && it !in ancestors },
            )
        }
    }
}

/**
 * The source sets of a consumer's compilation, other than its default one, that only its own target
 * compiles, so only this consumer can emit their fakes: the platform ancestors of the default set
 * and, for an Android variant, the `androidMain` (and flavor) sets it lists next to it. Computed
 * when the consumer's context is encoded, with the final graph. Empty for every other shape and for
 * non-KMP projects.
 */
internal fun platformOwnedAncestorsOf(
    compilation: KotlinCompilation<*>,
    shape: TaskShape,
): Set<String> {
    val kmp = compilation.project.extensions.findByType(KotlinMultiplatformExtension::class.java)
    if (shape != TaskShape.CONSUMER || kmp == null) return emptySet()
    val default = compilation.defaultSourceSet
    val ancestors = compilation.allKotlinSourceSets.mapTo(linkedSetOf()) { it.name } - default.name
    return platformOwnedAncestors(
        assignSourceSetOwners(readSourceSetGraph(kmp)),
        compilation.target.targetName,
        ancestors,
    )
}

/** The warning for a confirmed synthetic owner whose representative cannot be driven. */
internal fun unsupportedSyntheticOwnerMessage(
    owner: SourceSetOwner.Synthetic,
    platformType: String,
): String =
    "Fakt: the '${owner.sourceSet}' source set is shared only by $platformType targets " +
        "(representative '${owner.target}'), so its fakes are not generated yet. Move its @Fake " +
        "declarations to commonMain or to a platform source set."
