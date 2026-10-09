// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

/**
 * One compilation of a target, reduced to the source set names the ownership rules need.
 *
 * @property default The compilation's default source set.
 * @property members Every source set the compilation lists directly (KGP `kotlinSourceSets`),
 *   default included.
 * @property isTest True for a test-like compilation.
 */
internal data class CompilationFacts(
    val default: String,
    val members: List<String>,
    val isTest: Boolean,
)

/**
 * The source sets of the non-test compilations of one target.
 *
 * @property defaults The default source set of each compilation.
 * @property members Every other source set those compilations list directly, in first seen order.
 */
internal data class VariantSourceSets(val defaults: List<String>, val members: List<String>)

/**
 * Splits the non-test compilations into their default source sets and the members they list next to
 * them, for instance `androidMain` next to `androidDebug`. Test compilations are ignored.
 */
internal fun variantSourceSets(compilations: List<CompilationFacts>): VariantSourceSets {
    val mains = compilations.filterNot { it.isTest }
    val defaults = mains.map { it.default }
    return VariantSourceSets(
        defaults = defaults,
        members = mains.flatMap { it.members }.filterNot { it in defaults }.distinct(),
    )
}

/**
 * Builds the [TargetNode] of one target from its compilations. The main source sets are the
 * defaults of the non-test compilations (the target's own `<name>Main` when there is none).
 */
internal fun targetNodeOf(
    name: String,
    platformType: String,
    isAndroid: Boolean,
    compilations: List<CompilationFacts>,
): TargetNode {
    val sets = variantSourceSets(compilations)
    return TargetNode(
        name = name,
        platformType = platformType,
        isAndroid = isAndroid,
        mainSourceSets = sets.defaults.ifEmpty { listOf("${name}Main") },
        memberSourceSets = sets.members,
    )
}
