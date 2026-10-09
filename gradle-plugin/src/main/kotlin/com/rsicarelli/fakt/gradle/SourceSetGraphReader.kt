// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.android.AndroidIntegration
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget

private const val MAIN_COMPILATION = "main"
private const val COMMON_MAIN = "commonMain"

/**
 * Reads the live KMP model into the plain [SourceSetGraph] that [assignSourceSetOwners] decides on.
 * - `targets`: every real target (never the metadata target), each with the default source set of
 *   its non-test compilations.
 * - `parents`: the `dependsOn` edges of every source set, plus `commonMain` above every main source
 *   set, because KGP wires it even before the default hierarchy edges exist.
 * - `metadataCompilations`: the source sets KGP built a non-main metadata compilation for.
 *
 * The read touches only this project's extension, so it is safe under Gradle Project Isolation.
 */
internal fun readSourceSetGraph(kmp: KotlinMultiplatformExtension): SourceSetGraph {
    val targets = kmp.targets.filter { it !is KotlinMetadataTarget }.map { it.toTargetNode() }
    val mainSets = targets.flatMapTo(linkedSetOf()) { it.mainSourceSets }
    val edges =
        kmp.sourceSets.associate { set -> set.name to set.dependsOn.map { it.name }.toSet() }
    val parents =
        (edges.keys + mainSets).associateWith { name ->
            val declared = edges[name].orEmpty()
            if (name in mainSets && name != COMMON_MAIN) declared + COMMON_MAIN else declared
        }
    val metadataCompilations =
        kmp.targets
            .filterIsInstance<KotlinMetadataTarget>()
            .flatMap { it.compilations }
            .filter { it.name != MAIN_COMPILATION }
            .mapTo(linkedSetOf()) { it.defaultSourceSet.name }
    return SourceSetGraph(targets, parents, metadataCompilations)
}

private fun KotlinTarget.toTargetNode(): TargetNode =
    TargetNode(
        name = targetName,
        platformType = platformType.name.lowercase(),
        isAndroid = isAndroidTarget(),
        mainSourceSets =
            compilations
                .filterNot { it.isTestCompilation() }
                .map { it.defaultSourceSet.name }
                .ifEmpty { listOf("${targetName}Main") },
    )

/**
 * A compilation is test-like when it is associated with another compilation (the KGP shape of a
 * custom `integration`/`e2e`/`benchmark` test compilation) or its name says so (Android variants
 * and the default `test` compilation). An unassociated custom compilation without "test" in its
 * name stays a main compilation.
 */
private fun KotlinCompilation<*>.isTestCompilation(): Boolean =
    associatedCompilations.isNotEmpty() || name.contains("test", ignoreCase = true)

private fun KotlinTarget.isAndroidTarget(): Boolean =
    platformType.name.equals("androidJvm", ignoreCase = true) ||
        AndroidIntegration.isKmpAndroidTarget(this)
