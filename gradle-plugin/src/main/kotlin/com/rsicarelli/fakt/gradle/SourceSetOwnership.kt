// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

/**
 * One Kotlin target, reduced to plain data so the ownership rules can be tested without Gradle.
 *
 * @property name Target name, for example `jvm` or `desktop`.
 * @property platformType Lowercased KGP platform type: `jvm`, `androidjvm`, `js`, `wasm`, `native`.
 * @property isAndroid True for an Android target.
 * @property mainSourceSets The default source set of every main (or variant) compilation.
 */
internal data class TargetNode(
    val name: String,
    val platformType: String,
    val isAndroid: Boolean,
    val mainSourceSets: List<String>,
)

/**
 * The targets plus the `dependsOn` edges between source sets (`child -> direct parents`).
 *
 * A target "compiles" a source set when the set is one of its main source sets or an ancestor of
 * one.
 *
 * @property metadataCompilations The source sets KGP already built a non-main metadata compilation
 *   for. A listed set is owned by the metadata producer whatever the prediction says, and so are
 *   its ancestors. A set that is not listed falls back to the prediction, because the compilation
 *   may simply not exist yet.
 */
internal data class SourceSetGraph(
    val targets: List<TargetNode>,
    val parents: Map<String, Set<String>>,
    val metadataCompilations: Set<String> = emptySet(),
)

/**
 * Who generates the fakes of one source set. Every source set gets exactly one owner, so a fake is
 * never generated twice and never dropped.
 */
internal sealed interface SourceSetOwner {
    /** Only one target compiles the set, so that target generates it. */
    data class Platform(val target: String) : SourceSetOwner

    /** KGP makes a metadata compilation for the set, and the metadata producer generates it. */
    data class Metadata(val sourceSet: String) : SourceSetOwner

    /**
     * Several targets compile the set but KGP makes no metadata compilation for it. One
     * representative [target] generates it, compiling [compilationSourceSet] as its main source.
     */
    data class Synthetic(
        val sourceSet: String,
        val target: String,
        val compilationSourceSet: String,
    ) : SourceSetOwner

    /** A source set shared only by native targets (handled later, see #152). */
    data class NativeShared(val sourceSet: String) : SourceSetOwner
}

private const val NATIVE_TYPE = "native"
private const val COMMON_MAIN = "commonMain"
private val JVM_TYPES = setOf("jvm", "androidjvm")

/**
 * Decides the owner of every source set that at least one main compilation reaches.
 *
 * The compilers of a set are the targets whose main source sets reach it through `parents`. With no
 * compiler the set is absent from the result. With one compiler it is a [SourceSetOwner.Platform].
 * With several, it is [SourceSetOwner.Metadata] (or [SourceSetOwner.NativeShared] when all are
 * native) if KGP built a metadata compilation for it (see [SourceSetGraph.metadataCompilations]) or
 * [kgpCreatesMetadataCompilation] predicts one, and otherwise [SourceSetOwner.Synthetic] on the
 * [chooseRepresentative] target. Ancestors of a built set count as built, because their compilers
 * are a superset. Apart from `commonMain`, a set that only JVM and Android JVM targets compile is
 * never predicted: KGP disables its metadata compilation, so it is synthetic.
 *
 * The function is pure: the same graph always gives the same owners, whatever the input order.
 */
internal fun assignSourceSetOwners(graph: SourceSetGraph): Map<String, SourceSetOwner> {
    val closures = graph.targets.associateWith { mainClosure(it.mainSourceSets, graph.parents) }
    val allSets = closures.values.flatten().toSortedSet()
    val built = mainClosure(graph.metadataCompilations, graph.parents)
    return allSets.associateWith { set ->
        val compilers = graph.targets.filter { set in closures.getValue(it) }
        ownerOf(set, compilers, set in built)
    }
}

private fun ownerOf(
    set: String,
    compilers: List<TargetNode>,
    metadataBuilt: Boolean,
): SourceSetOwner {
    val types = compilers.map { it.platformType }.toSet()
    return when {
        compilers.size == 1 -> SourceSetOwner.Platform(compilers.single().name)
        !metadataBuilt && !predictsMetadataCompilation(set, types, compilers.size) ->
            syntheticOwner(set, compilers)
        types == setOf(NATIVE_TYPE) -> SourceSetOwner.NativeShared(set)
        else -> SourceSetOwner.Metadata(set)
    }
}

/**
 * [kgpCreatesMetadataCompilation], except that KGP disables the metadata compilation of any set
 * other than `commonMain` that only JVM and Android JVM targets compile.
 */
private fun predictsMetadataCompilation(set: String, types: Set<String>, count: Int): Boolean =
    kgpCreatesMetadataCompilation(types, count) &&
        (set == COMMON_MAIN || !JVM_TYPES.containsAll(types))

private fun syntheticOwner(set: String, compilers: List<TargetNode>): SourceSetOwner.Synthetic {
    val representative = chooseRepresentative(compilers)
    return SourceSetOwner.Synthetic(
        sourceSet = set,
        target = representative.name,
        compilationSourceSet = representative.mainSourceSets.min(),
    )
}

private fun mainClosure(start: Collection<String>, parents: Map<String, Set<String>>): Set<String> {
    val seen = linkedSetOf<String>()
    val pending = ArrayDeque(start)
    while (pending.isNotEmpty()) {
        val next = pending.removeFirst()
        if (seen.add(next)) pending.addAll(parents[next].orEmpty())
    }
    return seen
}

/**
 * Mirrors KGP: it builds a metadata compilation for a shared source set only when the targets that
 * compile it have more than one distinct platform type, or are all native and come from more than
 * one target. A single target never gets one.
 */
internal fun kgpCreatesMetadataCompilation(types: Set<String>, targetCount: Int): Boolean =
    types.size > 1 || (types == setOf(NATIVE_TYPE) && targetCount > 1)

/**
 * Picks the one target that generates a synthetic source set: non-Android first, then by name. The
 * order does not depend on the input list, so every build (and every cache key) picks the same
 * target.
 */
internal fun chooseRepresentative(compilers: List<TargetNode>): TargetNode =
    compilers.sortedWith(compareBy({ it.isAndroid }, { it.name })).first()

/**
 * Finds the test source set that sees the fakes of [owned]: `xMain` becomes `xTest` and plain
 * `main` becomes `test` (the same mapping `SourceSetDiscovery` uses). Returns null when that test
 * source set does not exist.
 */
internal fun counterpartTestSourceSet(owned: String, testSourceSets: Set<String>): String? {
    val candidate =
        when {
            owned == "main" -> "test"
            owned.endsWith("Main") -> owned.removeSuffix("Main") + "Test"
            else -> return null
        }
    return candidate.takeIf { it in testSourceSets }
}
