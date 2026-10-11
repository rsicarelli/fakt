// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation

/**
 * Whether [compilation] is compiled by `K2Native`: a native leaf (`linuxX64Main`) or a shared
 * native compilation (`nativeMain`, `appleMain`, and `commonMain` when every target is native). The
 * compiler driver follows the compilation type, never the source set name.
 */
internal fun isNativeCompilation(compilation: KotlinCompilation<*>): Boolean =
    compilation is KotlinNativeCompilation || compilation is KotlinSharedNativeCompilation

/**
 * The konan target names a native [compilation] is compiled for: its own target for a leaf, the
 * sorted set of its targets for a shared compilation, empty for anything else. Sorted and free of
 * the build host, so the list is a relocatable cache key. The dependency klibs the task also hashes
 * come from the host's distribution, so cross-OS cache hits are not guaranteed.
 */
internal fun konanTargetNames(compilation: KotlinCompilation<*>): List<String> =
    when (compilation) {
        is KotlinNativeCompilation -> listOf(compilation.konanTarget.name)
        is KotlinSharedNativeCompilation -> compilation.konanTargets.map { it.name }.sorted()
        else -> emptyList()
    }

/**
 * Gives [task] the Kotlin/Native inputs of [compilation]: where the distribution lives and which
 * version it is ([nativeDistribution]), the konan targets it is compiled for, and whether it is a
 * shared plan.
 *
 * Nothing here touches the file system or the compilation at configuration time. The targets are
 * read inside `project.provider`, when the task input resolves; the distribution comes from
 * providers only, and no task dependency is added, because KGP has already provisioned it.
 */
internal fun configureNativeInputs(task: FaktGenerateTask, compilation: KotlinCompilation<*>) {
    val distribution = nativeDistribution(task.project)
    task.konanHome.set(distribution.konanHome)
    task.kotlinNativeVersion.set(distribution.version)
    task.konanTargets.set(task.project.provider { konanTargetNames(compilation) })
    task.sharedNative.set(compilation is KotlinSharedNativeCompilation)
}
