// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.types

import com.rsicarelli.fakt.compiler.analysis.InterfaceAnalysis
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Handles import resolution for generated fake implementations.
 * Collects and resolves import statements needed for cross-module type references.
 *
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class ImportResolver(
    private val typeResolver: TypeResolver,
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
            val fqName = irClass.kotlinFqName.asString()
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
    }
}
