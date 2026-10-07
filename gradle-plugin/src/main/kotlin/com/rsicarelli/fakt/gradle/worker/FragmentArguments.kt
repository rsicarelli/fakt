// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import java.io.File

/**
 * The `-Xfragments` shape of one K2 invocation, in the exact syntax the compiler parses.
 *
 * @property fragments `-Xfragments` entries, ancestors before descendants.
 * @property sources `-Xfragment-sources` entries (`fragment:path`), one per source file.
 * @property refines `-Xfragment-refines` entries (`child:parent`).
 */
internal data class FragmentArguments(
    val fragments: List<String>,
    val sources: List<String>,
    val refines: List<String>,
)

/**
 * Builds the multi-fragment module structure for a JS/JVM invocation that analyses an intermediate
 * source set (`commonMain` -> `webMain` -> `jsMain`). Lumping the intermediate into
 * `-Xcommon-sources` makes an `actual` in it fail against a `commonMain` `expect` ("declared in the
 * same module"); one fragment per source set restores the real refinement structure.
 *
 * Only source sets that own at least one of [files] become fragments; refinement edges skip empty
 * ones, so each fragment refines its nearest non-empty ancestors. A file belongs to the source set
 * whose [roots] entry is its deepest containing directory.
 *
 * Returns `null` ("keep today's `-Xcommon-sources` arguments") when any file is under no known root
 * (the compiler rejects a source that belongs to no fragment) or when no intermediate level exists,
 * i.e. no fragment both refines another and is refined by another.
 */
internal fun buildFragmentArgs(
    sourceSets: List<SourceSetInfo>,
    files: List<File>,
    roots: Map<String, List<String>>,
): FragmentArguments? {
    val owners = files.map { it to owningSourceSet(it, roots) }
    if (owners.any { (_, set) -> set == null }) return null
    val ordered = topologicalOrder(sourceSets).filter { name -> owners.any { it.second == name } }
    val refines = collapsedRefines(sourceSets, ordered)
    val children = refines.map { it.first }.toSet()
    val hasIntermediate = refines.any { (_, parent) -> parent in children }
    val filesBySet = owners.groupBy({ it.second }, { it.first })
    return FragmentArguments(
            fragments = ordered,
            sources =
                ordered.flatMap { set ->
                    filesBySet.getValue(set).map { "$set:${it.absolutePath}" }
                },
            refines = refines.map { (child, parent) -> "$child:$parent" },
        )
        .takeIf { hasIntermediate }
}

/** Source set whose root is the deepest directory containing [file], or `null` when none does. */
private fun owningSourceSet(file: File, roots: Map<String, List<String>>): String? =
    roots.entries
        .flatMap { (name, dirs) -> dirs.map { name to it } }
        .filter { (_, dir) -> file.toPath().startsWith(File(dir).toPath()) }
        .maxByOrNull { (_, dir) -> File(dir).toPath().nameCount }
        ?.first

/** [sourceSets] ordered so every set follows all of its parents (stable for equal depth). */
private fun topologicalOrder(sourceSets: List<SourceSetInfo>): List<String> {
    val parentsByName = sourceSets.associate { it.name to it.parents }
    val ordered = LinkedHashSet<String>()

    fun visit(name: String) {
        if (name in ordered) return
        parentsByName[name].orEmpty().filter { it != name }.forEach(::visit)
        ordered.add(name)
    }
    sourceSets.forEach { visit(it.name) }
    return ordered.toList()
}

/** `(child, parent)` pairs among [kept], each child linked to its nearest kept ancestors. */
private fun collapsedRefines(
    sourceSets: List<SourceSetInfo>,
    kept: List<String>,
): List<Pair<String, String>> {
    val parentsByName = sourceSets.associate { it.name to it.parents }

    fun nearestKept(name: String, seen: MutableSet<String>): Set<String> =
        parentsByName[name]
            .orEmpty()
            .flatMap { parent ->
                when {
                    !seen.add(parent) -> emptyList()
                    parent in kept -> listOf(parent)
                    else -> nearestKept(parent, seen)
                }
            }
            .toSet()

    return kept.flatMap { child ->
        val parents = nearestKept(child, mutableSetOf())
        kept.filter { it in parents }.map { child to it }
    }
}

/**
 * The fragment structure for a run, or `null` to keep the flat `-Xcommon-sources` shape. Never for
 * the metadata driver, which rejects `-Xfragments` ("HMPP module structure should not be passed
 * during metadata compilation"), and never without common-fragment files.
 */
internal fun fragmentsFor(
    driver: CompilerDriver,
    commonFragmentFiles: List<File>,
    sourceSets: List<SourceSetInfo>,
    files: List<File>,
    roots: Map<String, List<String>>,
): FragmentArguments? =
    if (driver == CompilerDriver.METADATA || commonFragmentFiles.isEmpty()) {
        null
    } else {
        buildFragmentArgs(sourceSets, files, roots)
    }

/** Sets the three `-Xfragment*` arguments; all live on `CommonCompilerArguments`. */
internal fun applyFragmentArgs(bridge: K2CompilerBridge, args: Any, fragments: FragmentArguments) {
    val arrayType = Array<String>::class.java
    bridge.setOnArgs(args, "setFragments", arrayType, fragments.fragments.toTypedArray())
    bridge.setOnArgs(args, "setFragmentSources", arrayType, fragments.sources.toTypedArray())
    bridge.setOnArgs(args, "setFragmentRefines", arrayType, fragments.refines.toTypedArray())
}

/** Sets `-Xrefines-paths` (metadata driver arguments only); a no-op for an empty [paths]. */
internal fun applyRefinesPaths(bridge: K2CompilerBridge, args: Any, paths: List<File>) {
    if (paths.isEmpty()) return
    bridge.setOnArgs(
        args,
        "setRefinesPaths",
        Array<String>::class.java,
        paths.map { it.absolutePath }.toTypedArray(),
    )
}
