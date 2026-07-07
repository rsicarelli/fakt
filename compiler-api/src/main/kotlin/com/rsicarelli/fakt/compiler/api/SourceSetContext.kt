// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

import kotlinx.serialization.Serializable

/**
 * Complete source set context for a single Kotlin compilation.
 *
 * This data model is passed from the Gradle plugin to the compiler plugin via SubpluginOption
 * serialization.
 *
 * **Architecture**:
 * - Gradle plugin discovers project structure using KotlinCompilation APIs
 * - Serializes this context to JSON + Base64
 * - Compiler plugin deserializes and uses directly
 *
 * **Benefits**:
 * - Zero hardcoded source set names
 * - Automatically supports custom source sets (integrationTest, etc.)
 * - Works with Android build variants
 * - Future-proof for new Kotlin targets
 *
 * **Cross-Compilation Caching** (KMP optimization):
 * - `metadataOutputPath`: Set for metadata compilation (producer) to write cache
 * - `metadataCachePath`: Set for platform compilations (consumers) to read cache
 * - When cache is valid, platform compilations skip redundant FIR analysis
 *
 * **Per-Interface Output Directory** (KMP source set isolation):
 * - `outputDirectory`: Platform-specific output (e.g., jvmTest, iosTest)
 * - `commonTestOutputDirectory`: For cached (common) interfaces (commonTest)
 * - Interfaces from cache → commonTestOutputDirectory
 * - Platform-specific interfaces → outputDirectory
 *
 * @property compilationName Name of the compilation (e.g., "main", "test", "integrationTest")
 * @property targetName Name of the target (e.g., "jvm", "iosX64", "metadata")
 * @property platformType Platform type identifier (e.g., "jvm", "native", "js", "common")
 * @property isTest Whether this is a test compilation (vs production code)
 * @property defaultSourceSet The primary source set for this compilation
 * @property allSourceSets All source sets in the dependsOn hierarchy (including default)
 * @property outputDirectory Absolute path for platform-specific generated code
 * @property commonTestOutputDirectory Absolute path for cached (common) interfaces
 * @property metadataOutputPath Path to write FIR cache (producer mode: metadata compilation only)
 * @property metadataCachePath Path to read FIR cache (consumer mode: platform compilations)
 * @property emitPhase Compilation phase that writes generated fake sources (see [EmitPhase])
 * @property emitSourceSets Source sets whose `@Fake` declarations this compilation may emit. Empty
 *   (the default, omitted from serialized JSON) means "emit everything analyzed" — the legacy path
 *   and producer invocations. The cache-correct worker sets it at execution time for
 *   source-partitioned consumers that additionally feed ancestor sources (via `-Xcommon-sources`)
 *   for expect/actual and common-type resolution: those ancestor declarations are analysis-only,
 *   and emitting them would duplicate the common producer's output.
 * @see SourceSetInfo
 */
@Serializable
data class SourceSetContext(
    val compilationName: String,
    val targetName: String,
    val platformType: String,
    val isTest: Boolean,
    val defaultSourceSet: SourceSetInfo,
    val allSourceSets: List<SourceSetInfo>,
    val outputDirectory: String,
    val commonTestOutputDirectory: String,
    val metadataOutputPath: String? = null,
    val metadataCachePath: String? = null,
    val emitPhase: EmitPhase = EmitPhase.IR,
    val emitSourceSets: List<String> = emptyList(),
) {
    init {
        require(compilationName.isNotBlank()) { "compilationName cannot be blank" }
        require(targetName.isNotBlank()) { "targetName cannot be blank" }
        require(platformType.isNotBlank()) { "platformType cannot be blank" }
        require(outputDirectory.isNotBlank()) { "outputDirectory cannot be blank" }
        require(commonTestOutputDirectory.isNotBlank()) {
            "commonTestOutputDirectory cannot be blank"
        }
        require(allSourceSets.isNotEmpty()) { "allSourceSets cannot be empty" }
        require(allSourceSets.contains(defaultSourceSet)) {
            "allSourceSets must contain defaultSourceSet"
        }
    }
}

/**
 * Compilation phase that writes generated fake sources to disk.
 *
 * Selected explicitly rather than inferred from producer/consumer cache paths, because the legacy
 * in-process path also sets
 * [SourceSetContext.metadataOutputPath]/[SourceSetContext.metadataCachePath] (via its attachment to
 * the real KGP metadata compilation) and must keep emitting from the IR phase.
 * - [IR] — the default: `UnifiedFaktIrGenerationExtension` writes fakes during fir2ir. Used by the
 *   legacy in-process path and, until the FIR emitter ships, by the cache-correct worker too.
 * - [FIR] — the FIR checkers write fakes right after metadata extraction. Required for the
 *   `KotlinMetadataCompiler`-driven commonMain producer (the metadata pipeline has no IR phase) and
 *   set only by the cache-correct worker at execution time.
 *
 * The default is deliberately omitted from serialized JSON (all writers use `encodeDefaults=false`
 * Json instances) so legacy `@Input` payloads stay byte-identical.
 */
@Serializable
enum class EmitPhase {
    /** Generated fakes are written by the FIR checkers (metadata-driver producer path). */
    FIR,

    /** Generated fakes are written by the IR generation extension (default, legacy behavior). */
    IR,
}

/**
 * Information about a single Kotlin source set and its position in the hierarchy.
 *
 * **Key Concept**: The `parents` list represents the direct `dependsOn` relationships. For example,
 * if `jvmMain.dependsOn(commonMain)`, then:
 * - `SourceSetInfo(name = "jvmMain", parents = ["commonMain"])`
 *
 * **Hierarchy Example**:
 * ```
 * iosX64Main → iosMain → appleMain → nativeMain → commonMain
 *
 * Becomes:
 * - iosX64Main: parents = ["iosMain"]
 * - iosMain: parents = ["appleMain"]
 * - appleMain: parents = ["nativeMain"]
 * - nativeMain: parents = ["commonMain"]
 * - commonMain: parents = []  (root)
 * ```
 *
 * @property name Source set name (e.g., "jvmMain", "commonTest", "integrationTest")
 * @property parents Direct parent source sets from dependsOn relationships
 */
@Serializable
data class SourceSetInfo(val name: String, val parents: List<String>) {
    init {
        require(name.isNotBlank()) { "name cannot be blank" }
    }
}
