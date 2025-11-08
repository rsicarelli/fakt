// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.config.FaktOptions
import com.rsicarelli.fakt.compiler.fir.FirMetadataStorage

/**
 * Shared context passed between FIR and IR compilation phases.
 *
 * Following Metro pattern (see MetroCompilerPluginRegistrar.kt:24-57):
 * - Shared `classIds` and `options` objects
 * - Created once in CompilerPluginRegistrar
 * - Passed to both FIR and IR extensions
 *
 * **Design Rationale**:
 * Metro demonstrates that shared data structures are the idiomatic way to pass
 * information between compiler phases. This is simpler and more type-safe than
 * serialization or global state.
 *
 * **Lifecycle**:
 * 1. Created in [FaktCompilerPluginRegistrar.registerExtensions]
 * 2. Passed to [FaktFirExtensionRegistrar] (FIR writes metadata)
 * 3. Passed to [UnifiedFaktIrGenerationExtension] (IR reads metadata)
 * 4. Garbage collected after compilation completes
 *
 * **Thread Safety**:
 * - `fakeAnnotations` and `options`: Immutable, safe to share
 * - `metadataStorage`: Thread-safe ConcurrentHashMap internally
 *
 * **Visibility**: Public to allow FIR/IR extensions to access it (Metro pattern)
 *
 * @property fakeAnnotations List of @Fake annotation FQNs to detect (e.g., ["com.rsicarelli.fakt.Fake"])
 * @property options Compiler plugin options (log level, output dir, feature flags)
 * @property metadataStorage Storage for FIR→IR metadata passing
 */
data class FaktSharedContext(
    val fakeAnnotations: List<String>,
    val options: FaktOptions,
    val metadataStorage: FirMetadataStorage,
) {
    /**
     * Check if specific annotation FQN is configured for fake generation.
     *
     * Used by FIR checkers to determine if a declaration should be processed.
     *
     * @param annotationFqn Annotation fully qualified name
     * @return true if annotation is in the configured list
     */
    fun isConfiguredAnnotation(annotationFqn: String): Boolean = annotationFqn in fakeAnnotations

    companion object {
        /**
         * Default fake annotations.
         *
         * Currently only the official @Fake annotation, but could be extended
         * for third-party annotations or custom markers.
         */
        val DEFAULT_FAKE_ANNOTATIONS = listOf("com.rsicarelli.fakt.Fake")
    }
}
