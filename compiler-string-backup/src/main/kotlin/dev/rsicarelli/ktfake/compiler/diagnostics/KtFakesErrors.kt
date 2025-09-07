// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.diagnostics

/**
 * Diagnostic errors and warnings for KtFakes compiler plugin.
 *
 * Provides clear, actionable error messages following Kotlin compiler conventions.
 * All errors include helpful guidance on how to fix the issue.
 *
 * TODO: Implement proper FIR diagnostic infrastructure in next iteration
 */
object KtFakesErrors {

    const val FAKE_OBJECT_NOT_ALLOWED_MESSAGE =
        "Object declarations with @Fake annotation are not thread-safe. Use 'interface' or 'class' instead."

    const val FAKE_CONCURRENT_FALSE_WARNING_MESSAGE =
        "Setting concurrent=false may cause race conditions in parallel tests. Consider using scoped instances instead."

    const val FAKE_UNSUPPORTED_DECLARATION_TYPE_MESSAGE =
        "KtFake only supports interfaces and classes. Found: %s."

    const val FAKE_ANNOTATION_MISSING_TARGET_MESSAGE =
        "@Fake annotation target not found: %s."
}
