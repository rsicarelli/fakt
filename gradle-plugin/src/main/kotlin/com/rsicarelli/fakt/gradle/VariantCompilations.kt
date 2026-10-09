// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

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

/**
 * The source sets a consumer task owns and the ones it only analyses.
 *
 * @property own Every source set the compilation lists directly (KGP `kotlinSourceSets`): for an
 *   Android variant `androidDebug` and `androidMain` (plus flavor and build type sets), for any
 *   other compilation just its default source set.
 * @property analysisOnly The rest of the compilation's sources, `commonMain` and the shared
 *   intermediates (the common fragment).
 */
internal data class MemberSplit(
    val own: List<KotlinSourceSet>,
    val analysisOnly: List<KotlinSourceSet>,
)

/** Splits [compilation]'s source sets into the ones it lists directly and the shared rest. */
internal fun memberSplit(compilation: KotlinCompilation<*>): MemberSplit =
    MemberSplit(
        own = compilation.kotlinSourceSets.toList(),
        analysisOnly = (compilation.allKotlinSourceSets - compilation.kotlinSourceSets).toList(),
    )

/**
 * Appends to [infos] the [extra] source sets whose names are not listed yet, sorted by name. The
 * existing entries keep their order, so a compilation that lists nothing outside its default
 * closure serializes exactly as before.
 */
internal fun appendMissingSourceSets(
    infos: List<SourceSetInfo>,
    extra: List<SourceSetInfo>,
): List<SourceSetInfo> {
    val known = infos.mapTo(hashSetOf()) { it.name }
    return infos + extra.filter { it.name !in known }.distinctBy { it.name }.sortedBy { it.name }
}

/** Every source set [compilation] compiles, with its sorted `dependsOn` names. */
internal fun KotlinCompilation<*>.sourceSetInfos(): List<SourceSetInfo> =
    allKotlinSourceSets.map { set ->
        SourceSetInfo(set.name, set.dependsOn.map { it.name }.sorted())
    }
