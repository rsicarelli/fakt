// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.context

import com.rsicarelli.fakt.compiler.core.config.FaktOptions
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage

/**
 * Shared context passed between FIR and IR compilation phases.
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
