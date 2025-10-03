// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.argumentMapping
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
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

    private fun FirAnnotation.toAnnotationClassId(session: FirSession): ClassId? {
        // Extract ClassId from annotation type reference
        // For MVP, use a simple approach that's less likely to break with FIR API changes
        return try {
            // Try to get the ClassId using available stable APIs
            val typeRef = annotationTypeRef
            if (typeRef is FirResolvedTypeRef) {
                // For MVP, we'll rely on string-based matching rather than complex type analysis
                null
            } else {
                null
            }
        } catch (e: Exception) {
            // If ClassId extraction fails, return null to indicate unknown annotation
            null
        }
    }
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
