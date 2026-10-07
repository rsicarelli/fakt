// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

/**
 * Picks the test source sets that receive the fakes of the owned source set [owned].
 *
 * The counterpart (`webMain` -> `webTest`) wins when a test compilation compiles it, because the
 * tests of every target below it then see the fakes through `dependsOn`. Otherwise the fakes go to
 * each [leafTestSourceSets] entry, the default source sets of the test compilations that belong to
 * a main compilation reaching [owned].
 */
internal fun testWiringFor(
    owned: String,
    compiledTestSourceSets: Set<String>,
    leafTestSourceSets: Set<String>,
): Set<String> =
    counterpartTestSourceSet(owned, compiledTestSourceSets)?.let { setOf(it) } ?: leafTestSourceSets

/** The synthetic owners whose representative is [targetName], ordered by source set name. */
internal fun syntheticOwnersOf(
    owners: Map<String, SourceSetOwner>,
    targetName: String,
): List<SourceSetOwner.Synthetic> =
    owners.values
        .filterIsInstance<SourceSetOwner.Synthetic>()
        .filter { it.target == targetName }
        .sortedBy { it.sourceSet }

/**
 * The [ancestors] that only [target] compiles, so only that target's consumer can emit them (for
 * example `sharedJvmMain` in a jvm + js project). Keeps the order of [ancestors].
 */
internal fun platformOwnedAncestors(
    owners: Map<String, SourceSetOwner>,
    target: String,
    ancestors: Set<String>,
): Set<String> = ancestors.filterTo(linkedSetOf()) { owners[it] == SourceSetOwner.Platform(target) }
