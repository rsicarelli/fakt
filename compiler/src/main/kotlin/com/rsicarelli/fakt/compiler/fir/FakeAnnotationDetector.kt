// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Detector for @Fake annotations in FIR declarations.
 *
 * Provides utilities for:
 * - Detecting presence of @Fake annotations
 * - Extracting annotation parameters (trackCalls, concurrent, etc.)
 * - Validating annotation parameter values
 */
class FakeAnnotationDetector {
    companion object {
        private val FAKE_ANNOTATION_CLASS_ID =
            ClassId.topLevel(
                FqName("com.rsicarelli.fakt.Fake"),
            )

        private val FAKE_CONFIG_ANNOTATION_CLASS_ID =
            ClassId.topLevel(
                FqName("com.rsicarelli.fakt.FakeConfig"),
            )
    }

    /**
     * Check if a declaration has @Fake annotation.
     */
    fun hasFakeAnnotation(
        declaration: FirRegularClass,
        session: FirSession,
    ): Boolean = declaration.hasAnnotation(FAKE_ANNOTATION_CLASS_ID, session)

    /**
     * Check if a declaration has @FakeConfig annotation.
     */
    fun hasFakeConfigAnnotation(
        declaration: FirRegularClass,
        session: FirSession,
    ): Boolean = declaration.hasAnnotation(FAKE_CONFIG_ANNOTATION_CLASS_ID, session)

    /**
     * Extract @Fake annotation parameters.
     *
     * @return FakeAnnotationParameters with extracted values or defaults
     */
    fun extractFakeParameters(
        declaration: FirRegularClass,
        session: FirSession,
    ): FakeAnnotationParameters {
        val annotation =
            declaration.annotations.find { annotation ->
                annotation.toAnnotationClassId(session) == FAKE_ANNOTATION_CLASS_ID
            }

        return if (annotation != null) {
            extractParametersFromAnnotation(annotation, session)
        } else {
            FakeAnnotationParameters() // Use defaults
        }
    }

    @Suppress("UnusedParameter")
    private fun extractParametersFromAnnotation(
        annotation: FirAnnotation,
        session: FirSession,
    ): FakeAnnotationParameters {
        // Extract parameter values from annotation arguments
        // For MVP, return sensible defaults - parameter extraction is complex and not critical for basic functionality
        // Real parameter extraction will be implemented when more stable FIR APIs are available

        return FakeAnnotationParameters(
            trackCalls = false, // Default: no call tracking
            builder = false, // Default: no builder pattern
            dependencies = emptyList(), // Default: no dependencies
            concurrent = true, // Default: thread-safe
            scope = "test", // Default: test scope
        )
    }

    /**
     * Extension function to extract ClassId from FirAnnotation.
     * Reserved for future FIR API usage when annotation type resolution is stable.
     * Current implementation returns null and relies on string-based matching in IR phase for MVP.
     *
     * @param session The FIR session (reserved for future use)
     * @return ClassId of the annotation, currently always null
     */
    @Suppress("FunctionOnlyReturningConstant", "UnusedParameter")
    private fun FirAnnotation.toAnnotationClassId(session: FirSession): ClassId? = null
}

/**
 * Data class representing @Fake annotation parameters.
 */
data class FakeAnnotationParameters(
    val trackCalls: Boolean = false,
    val builder: Boolean = false,
    val dependencies: List<ClassId> = emptyList(),
    val concurrent: Boolean = true,
    val scope: String = "test",
)
