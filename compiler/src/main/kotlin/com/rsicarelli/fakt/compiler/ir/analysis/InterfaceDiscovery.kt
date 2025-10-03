// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import com.rsicarelli.fakt.compiler.optimization.CompilerOptimizations
import com.rsicarelli.fakt.compiler.types.TypeInfo
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName

/**
 * Discovers interfaces annotated with fake annotations in a module.
 *
 * This class is responsible for traversing the IR module and finding
 * all interfaces that should have fake implementations generated.
 *
 * Separated from the main generator to follow Single Responsibility Principle.
 *
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class InterfaceDiscovery(
    private val optimizations: CompilerOptimizations,
    private val messageCollector: MessageCollector? = null,
) {
    /**
     * Discovers all interfaces in the module that should have fakes generated.
     *
     * This method traverses the IR module structure and identifies interfaces
     * annotated with any of the configured fake annotations.
     *
     * @param moduleFragment The IR module to search
     * @return List of interfaces that need fake implementations
     */
    fun discoverFakeInterfaces(moduleFragment: IrModuleFragment): List<IrClass> {
        val discoveredInterfaces = mutableListOf<IrClass>()

        // Traverse all files in the module
        moduleFragment.files.forEach { file ->
            file.declarations.forEach { declaration ->
                if (isValidFakeInterface(declaration)) {
                    val matchingAnnotation = findMatchingAnnotation(declaration as IrClass)

                    if (matchingAnnotation != null) {
                        discoveredInterfaces.add(declaration)

                        // Create TypeInfo for optimization tracking
                        val typeInfo = createTypeInfo(declaration, file)
                        optimizations.indexType(typeInfo)

                        messageCollector?.reportInfo(
                            "Fakt: Discovered interface with $matchingAnnotation: ${declaration.name}",
                        )
                    }
                }
            }
        }

        messageCollector?.reportInfo("Fakt: Found ${discoveredInterfaces.size} fake interfaces to process")
        return discoveredInterfaces
    }

    /**
     * Checks if a declaration is a valid interface for fake generation.
     *
     * @param declaration The declaration to check
     * @return true if it's a valid interface, false otherwise
     */
    private fun isValidFakeInterface(declaration: org.jetbrains.kotlin.ir.declarations.IrDeclaration): Boolean =
        declaration is IrClass &&
            declaration.kind == ClassKind.INTERFACE &&
            declaration.origin != IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

    /**
     * Finds a matching fake annotation on the interface.
     *
     * @param declaration The interface to check
     * @return The fully qualified name of the matching annotation, or null if none found
     */
    private fun findMatchingAnnotation(declaration: IrClass): String? {
        val matchingAnnotation =
            declaration.annotations.find { annotation ->
                val annotationFqName = annotation.type.classFqName?.asString()
                annotationFqName != null && optimizations.isConfiguredFor(annotationFqName)
            }
        return matchingAnnotation?.type?.classFqName?.asString()
    }

    /**
     * Creates TypeInfo metadata for the discovered interface.
     *
     * @param declaration The interface declaration
     * @param file The IR file containing the interface
     * @return TypeInfo object for optimization tracking
     */
    private fun createTypeInfo(
        declaration: IrClass,
        file: org.jetbrains.kotlin.ir.declarations.IrFile,
    ): TypeInfo =
        TypeInfo(
            name = declaration.name.asString(),
            fullyQualifiedName = declaration.kotlinFqName.asString(),
            packageName = declaration.packageFqName?.asString() ?: "",
            fileName = file.fileEntry.name,
            annotations = declaration.annotations.mapNotNull { it.type.classFqName?.asString() },
            signature = computeInterfaceSignature(declaration),
        )

    /**
     * Computes a signature for an interface for change detection.
     *
     * The signature includes basic structural information to detect changes
     * between compilation runs for incremental compilation.
     *
     * @param irClass The interface to compute signature for
     * @return Signature string for change detection
     */
    private fun computeInterfaceSignature(irClass: IrClass): String {
        // Simplified signature computation to avoid deprecated API issues
        val signature = StringBuilder()
        signature.append("interface ${irClass.kotlinFqName}")

        // Add basic member count for change detection
        val propertyCount = irClass.declarations.filterIsInstance<IrProperty>().size
        val functionCount = irClass.declarations.filterIsInstance<IrSimpleFunction>().size
        signature.append("|props:$propertyCount|funs:$functionCount")

        return signature.toString()
    }

    private fun MessageCollector.reportInfo(message: String) {
        this.report(
            org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO,
            message,
            null,
        )
    }
}
