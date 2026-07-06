// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.context

import com.rsicarelli.fakt.compiler.core.config.FaktOptions
import com.rsicarelli.fakt.compiler.core.optimization.CompilerOptimizations
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.fir.cache.MetadataCacheManager
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage
import java.util.Collections

/**
 * Shared context passed between FIR and IR compilation phases.
 *
 * @property fakeAnnotations List of @Fake annotation FQNs to detect (e.g.,
 *   ["com.rsicarelli.fakt.Fake"])
 * @property options Compiler plugin options (log level, output dir, feature flags)
 * @property metadataStorage Storage for FIR→IR metadata passing
 * @property logger Logger instance for level-aware logging across compilation phases
 * @property optimizations Compiler optimizations instance for caching and incremental compilation
 * @property cacheManager Manager for cross-compilation FIR cache (KMP optimization)
 * @property emittedOutputs Thread-safe seen-set of `"$packageName:Fake${simpleName}Impl"` keys
 *   claimed by [com.rsicarelli.fakt.compiler.fir.generation.FirFakeEmitter] this compilation — the
 *   FIR-phase analogue of the IR path's `reportAndDropOutputCollisions`.
 */
data class FaktSharedContext(
    val fakeAnnotations: List<String>,
    val options: FaktOptions,
    val metadataStorage: FirMetadataStorage,
    val logger: FaktLogger,
    val optimizations: CompilerOptimizations,
    val cacheManager: MetadataCacheManager,
    val emittedOutputs: MutableSet<String> =
        Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap()),
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
         * Currently only the official @Fake annotation, but could be extended for third-party
         * annotations or custom markers.
         */
        val DEFAULT_FAKE_ANNOTATIONS = listOf("com.rsicarelli.fakt.Fake")
    }
}
