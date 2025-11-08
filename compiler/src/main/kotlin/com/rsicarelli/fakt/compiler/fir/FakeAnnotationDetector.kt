// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Detector for @Fake annotations in FIR declarations.
 *
 * ## FIR Phase Role
 *
 * This class operates in the FIR (Frontend Intermediate Representation) phase of Kotlin compilation,
 * where annotation detection and type resolution are most accurate. It provides the foundation
 * for FakeInterfaceChecker and FakeClassChecker to identify @Fake annotated declarations.
 *
 * @see com.rsicarelli.fakt.compiler.fir.FakeInterfaceChecker for usage in interface validation
 * @see com.rsicarelli.fakt.compiler.fir.FakeClassChecker for usage in class validation
 */
class FakeAnnotationDetector {
    companion object {
        private val FAKE_ANNOTATION_CLASS_ID =
            ClassId.topLevel(
                FqName("com.rsicarelli.fakt.Fake"),
            )
    }

    /**
     * Check if a declaration has @Fake annotation.
     *
     * Uses stable FIR API `hasAnnotation()` for reliable detection across Kotlin versions.
     *
     * **Performance**: O(a) where a = number of annotations (typically < 5)
     * Typical cost: < 10μs per declaration
     *
     * @param declaration FIR class declaration to check (interface or class)
     * @param session FIR session for annotation resolution
     * @return true if declaration is annotated with @Fake, false otherwise
     */
    fun hasFakeAnnotation(
        declaration: FirRegularClass,
        session: FirSession,
    ): Boolean = declaration.hasAnnotation(FAKE_ANNOTATION_CLASS_ID, session)
}