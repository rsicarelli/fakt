// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.context

import com.rsicarelli.fakt.compiler.core.types.TypeResolution
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Handles import resolution for generated fake implementations.
 * Collects and resolves import statements needed for cross-module type references.
 */
internal class ImportResolver(
    private val typeResolver: TypeResolution,
) {
    /**
     * Collect all required import statements for types used in the interface.
     * This fixes cross-module import resolution by analyzing all type references.
     *
     * @param analysis The interface analysis containing all types to resolve
     * @param currentPackage The package of the generated fake class
     * @return Set of fully qualified type names that need to be imported
     */
    fun collectRequiredImports(
        analysis: InterfaceAnalysis,
        currentPackage: String,
    ): Set<String> {
        val imports = mutableSetOf<String>()

        // Collect imports from function return types and parameters
        for (function in analysis.functions) {
            collectImportsFromType(function.returnType, currentPackage, imports)
            for (parameter in function.parameters) {
                collectImportsFromType(parameter.type, currentPackage, imports)
            }
        }

        // Collect imports from property types
        for (property in analysis.properties) {
            collectImportsFromType(property.type, currentPackage, imports)
        }

        return imports
    }

    /**
     * Collect all required import statements for types used in the class.
     * Similar to collectRequiredImports but for class analysis (abstract/final classes).
     *
     * @param analysis The class analysis containing all types to resolve
     * @param currentPackage The package of the generated fake class
     * @return Set of fully qualified type names that need to be imported
     */
    fun collectRequiredImportsForClass(
        analysis: ClassAnalysis,
        currentPackage: String,
    ): Set<String> {
        val imports = mutableSetOf<String>()

        // Collect imports from abstract methods
        for (function in analysis.abstractMethods) {
            collectImportsFromType(function.returnType, currentPackage, imports)
            for (parameter in function.parameters) {
                collectImportsFromType(parameter.type, currentPackage, imports)
            }
        }

        // Collect imports from open methods
        for (function in analysis.openMethods) {
            collectImportsFromType(function.returnType, currentPackage, imports)
            for (parameter in function.parameters) {
                collectImportsFromType(parameter.type, currentPackage, imports)
            }
        }

        // Collect imports from abstract properties
        for (property in analysis.abstractProperties) {
            collectImportsFromType(property.type, currentPackage, imports)
        }

        // Collect imports from open properties
        for (property in analysis.openProperties) {
            collectImportsFromType(property.type, currentPackage, imports)
        }

        return imports
    }

    /**
     * Extract import requirements from an IR type.
     * Handles both simple types and generic types with parameters.
     *
     * @param irType The IR type to analyze
     * @param currentPackage The current package context
     * @param imports Mutable set to collect import requirements
     */
    private fun collectImportsFromType(
        irType: IrType,
        currentPackage: String,
        imports: MutableSet<String>,
    ) {
        // Skip primitive types - they don't need imports
        if (typeResolver.isPrimitiveType(irType)) {
            return
        }

        val irClass = irType.getClass()
        if (irClass != null) {
            val resolvedFqName = irClass.kotlinFqName.asString()

            // Map JVM stdlib types to Kotlin equivalents for platform-agnostic code
            val fqName = mapJvmTypeToKotlin(resolvedFqName)
            val packageName = fqName.substringBeforeLast('.', "")

            // Only add import if it's from a different package and not kotlin.* built-ins
            if (shouldImportType(packageName, currentPackage)) {
                imports.add(fqName)
            }

            // Handle generic type parameters (for future generic support)
            if (irType is IrSimpleType) {
                for (typeArgument in irType.arguments) {
                    if (typeArgument is IrTypeProjection) {
                        collectImportsFromType(typeArgument.type, currentPackage, imports)
                    }
                }
            }
        }
    }

    /**
     * Determines if a type should be imported based on package rules.
     *
     * @param typePackage Package of the type being considered
     * @param currentPackage Package of the generated class
     * @return true if the type should be imported, false otherwise
     */
    private fun shouldImportType(
        typePackage: String,
        currentPackage: String,
    ): Boolean =
        typePackage.isNotEmpty() &&
            typePackage != currentPackage &&
            !typePackage.startsWith("kotlin.") &&
            !isKotlinBuiltIn(typePackage)

    /**
     * Checks if a package contains Kotlin built-in types that don't need imports.
     *
     * @param packageName The package to check
     * @return true if this is a built-in package, false otherwise
     */
    private fun isKotlinBuiltIn(packageName: String): Boolean = packageName in KOTLIN_BUILTIN_PACKAGES

    companion object {
        /**
         * Set of Kotlin packages that contain built-in types and don't require explicit imports.
         */
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

        /**
         * Maps JVM-specific stdlib types to their Kotlin equivalents.
         *
         * The Kotlin compiler resolves typealiases to their platform-specific implementations
         * (e.g., kotlin.Exception → java.lang.Exception on JVM). This causes problems when
         * generating code for common source sets (commonMain/commonTest) that must be
         * platform-agnostic.
         *
         * This map ensures we always use kotlin.* types in generated imports, which work
         * across all Kotlin platforms (JVM, Native, JS, Wasm).
         */
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
                "java.util.ConcurrentModificationException" to "kotlin.collections.ConcurrentModificationException",
                // Collections (rare, but possible in function signatures)
                "java.lang.Comparable" to "kotlin.Comparable",
                "java.lang.CharSequence" to "kotlin.CharSequence",
                "java.lang.Appendable" to "kotlin.text.Appendable",
                "java.lang.Number" to "kotlin.Number",
            )
    }

    /**
     * Maps JVM-specific stdlib types to their Kotlin equivalents.
     *
     * This ensures generated code uses platform-agnostic kotlin.* types instead of
     * JVM-specific java.lang.* types, allowing the code to compile on all KMP targets.
     *
     * @param fqName The fully qualified name to map (e.g., "java.lang.Exception")
     * @return Kotlin equivalent if mapped, original FQN otherwise
     */
    private fun mapJvmTypeToKotlin(fqName: String): String = JVM_TO_KOTLIN_TYPE_MAP[fqName] ?: fqName
}
