// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.checkers

/**
 * FIR diagnostic errors for @Fake annotation validation for @Fake validation.
 *
 * **Simplified Error Reporting**: Instead of using complex FIR diagnostic factories
 * (which may vary across Kotlin versions), we use direct error reporting via
 * session.messageCollector which is stable across versions.
 *
 * This approach:
 * - Provides clear error messages at compilation time
 * - Works consistently across Kotlin compiler versions
 * - Follows pragmatic compiler plugin development practices
 *
 * **Error Naming Convention**:
 * - FAKE_* for @Fake annotation errors
 * - Clear, actionable messages
 */
internal object FirFaktErrors {
    const val FAKE_MUST_BE_INTERFACE = "[FAKT] @Fake can only be applied to interfaces, not classes or objects"
    const val FAKE_CANNOT_BE_SEALED = "[FAKT] @Fake cannot be applied to sealed interfaces"
    const val FAKE_CANNOT_BE_LOCAL = "[FAKT] @Fake cannot be applied to local classes or interfaces"
    const val FAKE_CANNOT_BE_EXPECT = "[FAKT] @Fake cannot be applied to expect declarations (KMP)"
    const val FAKE_CANNOT_BE_EXTERNAL = "[FAKT] @Fake cannot be applied to external declarations"

    // Updated class validation errors
    const val FAKE_CLASS_MUST_BE_ABSTRACT = "[FAKT] @Fake class must be abstract (contain abstract or open members)"
    const val FAKE_CLASS_CANNOT_BE_SEALED = "[FAKT] @Fake class cannot be sealed"
    const val FAKE_CLASS_CANNOT_BE_FINAL = "[FAKT] @Fake class cannot be final (must be abstract or open)"
    const val FAKE_OPEN_CLASS_NO_OPEN_MEMBERS = "[FAKT] @Fake open class must have at least one open property or method"
}
