// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import dev.rsicarelli.ktfake.compiler.diagnostics.KtFakesErrors

/**
 * FIR checker for thread-safety validation of @Fake annotations.
 *
 * Validates that:
 * - Object declarations are not annotated with @Fake (not thread-safe)
 * - @Fake annotations are only applied to supported declaration types
 * - Warns when concurrent=false is used (potential race conditions)
 *
 * Based on Metro's thread-safety requirements and the roadmap specification.
 */
class ThreadSafetyChecker : FirRegularClassChecker(MppCheckerKind.Common) {

    companion object {
        private val FAKE_ANNOTATION_CLASS_ID = ClassId.topLevel(
            FqName("dev.rsicarelli.ktfake.Fake")
        )
    }

    // TODO: Implement proper check method signature when FIR API is stable
    // For now, create a simple validation method to establish the structure
    fun validate(declaration: FirRegularClass, context: CheckerContext) {
        // Check if the class has @Fake annotation
        if (!declaration.hasAnnotation(FAKE_ANNOTATION_CLASS_ID, context.session)) {
            return
        }

        // Validate that object declarations are not allowed with @Fake
        if (declaration.classKind == ClassKind.OBJECT) {
            // TODO: Implement proper FIR diagnostic reporting
            // For now, we'll just validate the detection logic works
            println("KtFakes: Found @Fake on object declaration '${declaration.name}' - this should be reported as an error")
        }

        // TODO: Add validation for concurrent=false warning
        // TODO: Add validation for unsupported declaration types
    }
}
