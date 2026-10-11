// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import java.io.File

/**
 * The [NativeArgumentsInput] of a Native task, or `null` when the task has no konan targets. Built
 * from task data only; the host is read from the worker JVM's `os.name` / `os.arch` here, at
 * execution time, so it never reaches the cache key.
 *
 * Leaf compilations pass their `dependencies` and let K2Native add the distribution libraries;
 * shared compilations analyse [ownSources] as common sources.
 */
internal fun nativeArgumentsInput(
    params: FaktCodegenWorkParameters,
    ownSources: List<File>,
    dependencies: List<File>,
    refinesPaths: List<File>,
): NativeArgumentsInput? {
    val konanTargets = params.konanTargets.getOrElse(emptyList())
    if (konanTargets.isEmpty()) return null
    return NativeArgumentsInput(
        shared = params.sharedNative.getOrElse(false),
        konanTargets = konanTargets,
        hostTarget = hostKonanTarget(System.getProperty("os.name"), System.getProperty("os.arch")),
        konanHome = params.konanHome.get().asFile,
        dependencies = dependencies,
        ownSources = ownSources,
        refinesPaths = refinesPaths,
        scratchDir = params.scratchDir.asFile.get(),
    )
}
