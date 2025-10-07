// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

import kotlinx.serialization.Serializable

/**
 * Complete source set context for a single Kotlin compilation.
 *
 * This data model is passed from the Gradle plugin to the compiler plugin
 * via SubpluginOption serialization, replacing all hardcoded pattern matching
 * logic with programmatic discovery.
 *
 * **Architecture**:
 * - Gradle plugin discovers project structure using KotlinCompilation APIs
 * - Serializes this context to JSON + Base64
 * - Compiler plugin deserializes and uses directly (no pattern matching!)
 *
 * **Benefits**:
 * - Zero hardcoded source set names
 * - Automatically supports custom source sets (integrationTest, etc.)
 * - Works with Android build variants
 * - Future-proof for new Kotlin targets
 *
 * @property compilationName Name of the compilation (e.g., "main", "test", "integrationTest")
 * @property targetName Name of the target (e.g., "jvm", "iosX64", "metadata")
 * @property platformType Platform type identifier (e.g., "jvm", "native", "js", "common")
 * @property isTest Whether this is a test compilation (vs production code)
 * @property defaultSourceSet The primary source set for this compilation
 * @property allSourceSets All source sets in the dependsOn hierarchy (including default)
 * @property outputDirectory Absolute path where generated code should be written
 *
 * @since 1.1.0
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
) {
    init {
        require(compilationName.isNotBlank()) { "compilationName cannot be blank" }
        require(targetName.isNotBlank()) { "targetName cannot be blank" }
        require(platformType.isNotBlank()) { "platformType cannot be blank" }
        require(outputDirectory.isNotBlank()) { "outputDirectory cannot be blank" }
        require(allSourceSets.isNotEmpty()) { "allSourceSets cannot be empty" }
        require(allSourceSets.contains(defaultSourceSet)) {
            "allSourceSets must contain defaultSourceSet"
        }
    }
}

/**
 * Information about a single Kotlin source set and its position in the hierarchy.
 *
 * **Key Concept**: The `parents` list represents the direct `dependsOn` relationships.
 * For example, if `jvmMain.dependsOn(commonMain)`, then:
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
 *
 * @since 1.1.0
 */
@Serializable
data class SourceSetInfo(
    val name: String,
    val parents: List<String>,
) {
    init {
        require(name.isNotBlank()) { "name cannot be blank" }
    }
}
