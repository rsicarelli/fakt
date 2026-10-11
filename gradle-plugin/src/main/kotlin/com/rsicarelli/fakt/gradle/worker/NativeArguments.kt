// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import java.io.File

/**
 * Inputs of one `K2Native` analysis run.
 *
 * @property shared `true` for a shared-native compilation (`nativeMain`, `appleMain`, all-native
 *   `commonMain`), `false` for a leaf (`linuxX64Main`).
 * @property konanTargets `[t]` for a leaf, the host-independent sorted set for a shared
 *   compilation.
 * @property hostTarget [hostKonanTarget] of the machine running the worker.
 * @property konanHome the Kotlin/Native distribution root (`konan.home`).
 * @property dependencies klibs the compilation depends on, in compilation order.
 * @property ownSources the own source files; shared compilations analyse them as common sources.
 * @property refinesPaths ancestor metadata klibs a shared compilation refines.
 * @property scratchDir the task's `@LocalState` directory the throwaway klib is written under.
 */
internal data class NativeArgumentsInput(
    val shared: Boolean,
    val konanTargets: List<String>,
    val hostTarget: String,
    val konanHome: File,
    val dependencies: List<File>,
    val ownSources: List<File>,
    val refinesPaths: List<File>,
    val scratchDir: File,
)

/**
 * The `K2Native` argument values for a [NativeArgumentsInput], as plain strings so the plan is
 * unit-testable without compiler types. A `null` [commonSources] / empty [refinesPaths] means "set
 * nothing": a leaf's common fragment stays on the shared `populateConsumerMultiplatformArgs`.
 */
internal data class NativeArguments(
    val produce: String,
    val metadataKlib: Boolean,
    val target: String,
    val outputName: String,
    val libraries: List<String>,
    val noDefaultLibs: Boolean,
    val noStdlib: Boolean,
    val commonSources: List<String>?,
    val refinesPaths: List<String>,
    val multiPlatform: Boolean,
)

private const val ANALYSIS_KLIB = "native-klib/fakt-analysis"
private const val STDLIB_RELATIVE_PATH = "klib/common/stdlib"

/**
 * Plans the `K2Native` invocation.
 * - **Leaf**: default libs stay on (K2Native adds `klib/common/stdlib` and `klib/platform/<t>` from
 *   `konan.home`), so only the compilation's own dependencies go on `-library`.
 * - **Shared**: the platform libs of one concrete target are wrong for a shared set, so
 *   `-no-default-libs` and `-nostdlib` are set and the distribution stdlib is listed explicitly
 *   ahead of the (commonized) dependencies. The own sources are analysed as `-Xcommon-sources`
 *   against the ancestors' metadata klibs (`-Xrefines-paths`).
 */
internal fun nativeArguments(input: NativeArgumentsInput): NativeArguments {
    val representative = representativeKonanTarget(input.konanTargets, input.hostTarget)
    val outputName = File(input.scratchDir, ANALYSIS_KLIB).absolutePath
    return if (input.shared) {
        sharedArguments(input, representative, outputName)
    } else {
        leafArguments(input, representative, outputName)
    }
}

private fun leafArguments(
    input: NativeArgumentsInput,
    target: String,
    outputName: String,
): NativeArguments =
    NativeArguments(
        produce = "library",
        metadataKlib = true,
        target = target,
        outputName = outputName,
        libraries = input.dependencies.map { it.absolutePath },
        noDefaultLibs = false,
        noStdlib = false,
        commonSources = null,
        refinesPaths = emptyList(),
        multiPlatform = false,
    )

private fun sharedArguments(
    input: NativeArgumentsInput,
    target: String,
    outputName: String,
): NativeArguments =
    NativeArguments(
        produce = "library",
        metadataKlib = true,
        target = target,
        outputName = outputName,
        libraries = withDistributionStdlib(input.konanHome, input.dependencies),
        noDefaultLibs = true,
        noStdlib = true,
        commonSources = input.ownSources.map { it.absolutePath },
        refinesPaths = input.refinesPaths.map { it.absolutePath },
        multiPlatform = true,
    )

/**
 * The distribution stdlib followed by [dependencies], dropping any dependency that is the same file
 * as one already listed. Compared by [File.canonicalFile], never by string, so `..`, a trailing
 * separator or Windows separators cannot defeat it.
 */
private fun withDistributionStdlib(konanHome: File, dependencies: List<File>): List<String> {
    val seen = mutableSetOf<File>()
    return (listOf(File(konanHome, STDLIB_RELATIVE_PATH)) + dependencies)
        .filter { seen.add(it.canonicalFile) }
        .map { it.absolutePath }
}

/**
 * The target a shared compilation is analysed for: the [host] when it is one of [targets],
 * otherwise the first target in sorted order. Independent of the order of [targets], and the same
 * choice KGP makes for a shared metadata compilation.
 */
internal fun representativeKonanTarget(targets: List<String>, host: String): String {
    require(targets.isNotEmpty()) { "A native compilation needs at least one konan target." }
    return if (host in targets) host else targets.min()
}

/**
 * The Kotlin/Native target name of the machine described by the `os.name` / `os.arch` system
 * properties. Windows maps to `mingw_x64` (the only Windows host Kotlin/Native supports).
 */
internal fun hostKonanTarget(osName: String, osArch: String): String {
    val os = osName.lowercase()
    val arch = osArch.lowercase()
    val isArm = arch == "aarch64" || arch == "arm64"
    return when {
        os.startsWith("windows") -> "mingw_x64"
        os.startsWith("mac") -> if (isArm) "macos_arm64" else "macos_x64"
        os.startsWith("linux") -> if (isArm) "linux_arm64" else "linux_x64"
        else -> error("Unsupported Kotlin/Native host: os.name='$osName', os.arch='$osArch'.")
    }
}

/**
 * Sets the [plan] on a `K2NativeCompilerArguments` instance reflectively, through [bridge] like
 * every other driver. Only the setters Kotlin 2.2.0 and 2.4.10 share are used. Arguments absent
 * from the plan (a leaf's common fragment, empty refines paths) are left untouched.
 */
internal fun applyNativeArguments(bridge: K2CompilerBridge, args: Any, plan: NativeArguments) {
    val string = String::class.java
    val flag = Boolean::class.javaPrimitiveType!!
    val strings = Array<String>::class.java
    bridge.setOnArgs(args, "setProduce", string, plan.produce)
    bridge.setOnArgs(args, "setMetadataKlib", flag, plan.metadataKlib)
    bridge.setOnArgs(args, "setTarget", string, plan.target)
    bridge.setOnArgs(args, "setOutputName", string, plan.outputName)
    bridge.setOnArgs(args, "setLibraries", strings, plan.libraries.toTypedArray())
    if (plan.noDefaultLibs) bridge.setOnArgs(args, "setNodefaultlibs", flag, true)
    if (plan.noStdlib) bridge.setOnArgs(args, "setNostdlib", flag, true)
    if (plan.multiPlatform) bridge.setOnArgs(args, "setMultiPlatform", flag, true)
    plan.commonSources?.let {
        bridge.setOnArgs(args, "setCommonSources", strings, it.toTypedArray())
    }
    if (plan.refinesPaths.isNotEmpty()) {
        bridge.setOnArgs(args, "setRefinesPaths", strings, plan.refinesPaths.toTypedArray())
    }
}
