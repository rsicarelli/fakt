// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.context

/**
 * Filters and remaps a pre-collected set of fully-qualified type names into the import list for a
 * generated fake.
 *
 * Stateless: no IR walking — only string-set filtering (drops same-package and stdlib FQNs) plus a
 * JVM→Kotlin remap that keeps generated code platform-agnostic. Inputs are sourced from
 * [com.rsicarelli.fakt.codegen.analysis.FakeDeclaration.requiredImports].
 */
internal class ImportResolver {
    /**
     * @param fqns Fully-qualified type names referenced by the declaration.
     * @param currentPackage Package of the generated fake (imports in the same package are
     *   dropped).
     * @return Set of fully-qualified names that must be imported.
     */
    fun resolveImports(fqns: Set<String>, currentPackage: String): Set<String> =
        fqns
            .map { mapJvmTypeToKotlin(it) }
            .filter { fqName ->
                val packageName = fqName.substringBeforeLast('.', "")
                shouldImportType(packageName, currentPackage)
            }
            .toSet()

    /** True when the type's package is foreign and not a Kotlin built-in package. */
    private fun shouldImportType(typePackage: String, currentPackage: String): Boolean =
        typePackage.isNotEmpty() &&
            typePackage != currentPackage &&
            !typePackage.startsWith("kotlin.") &&
            typePackage !in KOTLIN_BUILTIN_PACKAGES

    /**
     * Maps JVM-specific stdlib types to their Kotlin equivalents.
     *
     * The Kotlin compiler resolves typealiases to their platform-specific implementations (e.g.,
     * `kotlin.Exception` → `java.lang.Exception` on JVM). When generating code for common source
     * sets (commonMain/commonTest), imports must reference the platform-agnostic `kotlin.*` names.
     */
    private fun mapJvmTypeToKotlin(fqName: String): String =
        JVM_TO_KOTLIN_TYPE_MAP[fqName] ?: fqName

    private companion object {
        /** Kotlin packages whose types are auto-imported and don't require explicit imports. */
        private val KOTLIN_BUILTIN_PACKAGES =
            setOf(
                "kotlin",
                "kotlin.collections",
                "kotlin.ranges",
                "kotlin.sequences",
                "kotlin.text",
                "kotlin.io",
                "kotlin.comparisons",
            )

        private val JVM_TO_KOTLIN_TYPE_MAP =
            mapOf(
                // Exceptions
                "java.lang.Throwable" to "kotlin.Throwable",
                "java.lang.Exception" to "kotlin.Exception",
                "java.lang.RuntimeException" to "kotlin.RuntimeException",
                "java.lang.Error" to "kotlin.Error",
                "java.lang.IllegalStateException" to "kotlin.IllegalStateException",
                "java.lang.IllegalArgumentException" to "kotlin.IllegalArgumentException",
                "java.lang.IndexOutOfBoundsException" to "kotlin.IndexOutOfBoundsException",
                "java.lang.UnsupportedOperationException" to "kotlin.UnsupportedOperationException",
                "java.lang.NumberFormatException" to "kotlin.NumberFormatException",
                "java.lang.NullPointerException" to "kotlin.NullPointerException",
                "java.lang.ClassCastException" to "kotlin.ClassCastException",
                "java.lang.AssertionError" to "kotlin.AssertionError",
                "java.lang.NoSuchElementException" to "kotlin.NoSuchElementException",
                "java.lang.ArithmeticException" to "kotlin.ArithmeticException",
                "java.util.ConcurrentModificationException" to
                    "kotlin.collections.ConcurrentModificationException",
                // Collections (rare, but possible in function signatures)
                "java.lang.Comparable" to "kotlin.Comparable",
                "java.lang.CharSequence" to "kotlin.CharSequence",
                "java.lang.Appendable" to "kotlin.text.Appendable",
                "java.lang.Number" to "kotlin.Number",
            )
    }
}
