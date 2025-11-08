// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import com.rsicarelli.fakt.compiler.FaktSharedContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * FIR checker for @Fake annotated classes (abstract/open classes).
 *
 * Following Metro pattern, validates that:
 * 1. Class must be a CLASS (not interface, object, etc.)
 * 2. Class must be abstract (has abstract or open members to fake)
 * 3. Class must not be sealed
 * 4. Class must not be local
 *
 * **Note**: Class faking is secondary to interface faking in Fakt.
 * The primary use case is interfaces, but we support abstract classes
 * for compatibility with existing code patterns.
 *
 * @property sharedContext Shared context with metadata storage
 */
internal class FakeClassChecker(
    private val sharedContext: FaktSharedContext,
) : FirClassChecker(MppCheckerKind.Common) {
    companion object {
        // @Fake annotation ClassId
        private val FAKE_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("com.rsicarelli.fakt.Fake"))
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        // Skip if FIR analysis not enabled (legacy mode)
        if (!sharedContext.useFirAnalysis()) return

        val session = context.session

        // Check if class has @Fake annotation
        if (!declaration.hasAnnotation(FAKE_ANNOTATION_CLASS_ID, session)) return

        // Skip if already validated as interface (FakeInterfaceChecker handles it)
        if (declaration.classKind == ClassKind.INTERFACE) return

        val source = declaration.source ?: return

        // Validate it's a class
        if (declaration.classKind != ClassKind.CLASS) {
            // Phase 3B.4: Objects, enum classes, etc. cannot be faked
            reportError(session, source, FirFaktErrors.FAKE_CLASS_MUST_BE_ABSTRACT)
            return // Skip non-classes
        }

        // Phase 3C.5: Validate class modality (abstract or open)
        when (declaration.modality) {
            Modality.ABSTRACT -> {
                // Allow abstract classes (existing behavior from Phase 3C.1)
            }
            Modality.OPEN -> {
                // Phase 3C.5: Allow open classes if they have open members
                if (!hasOpenMembers(declaration)) {
                    reportError(session, source, FirFaktErrors.FAKE_OPEN_CLASS_NO_OPEN_MEMBERS)
                    return
                }
            }
            Modality.FINAL -> {
                reportError(session, source, FirFaktErrors.FAKE_CLASS_CANNOT_BE_FINAL)
                return
            }
            Modality.SEALED -> {
                reportError(session, source, FirFaktErrors.FAKE_CLASS_CANNOT_BE_SEALED)
                return
            }
            null -> {
                // Treat null modality as FINAL
                reportError(session, source, FirFaktErrors.FAKE_CLASS_CANNOT_BE_FINAL)
                return
            }
        }

        // Validate not local
        if (declaration.symbol.classId.isLocal) {
            reportError(session, source, FirFaktErrors.FAKE_CANNOT_BE_LOCAL)
            return
        }

        // ✅ Validation passed - analyze and store metadata
        analyzeAndStoreMetadata(declaration, session)
    }

    /**
     * Check if class has any open members (properties or methods).
     *
     * Phase 3C.5: Open classes without open members cannot be faked
     * because there's nothing to override.
     *
     * @param declaration FIR class declaration to check
     * @return true if class has at least one open property or method
     */
    @OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
    private fun hasOpenMembers(declaration: FirClass): Boolean {
        var hasOpen = false

        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            when (symbol) {
                is org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol -> {
                    if (symbol.fir.modality == Modality.OPEN) {
                        hasOpen = true
                    }
                }
                is org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol -> {
                    if (symbol.fir.modality == Modality.OPEN) {
                        hasOpen = true
                    }
                }
            }
        }

        return hasOpen
    }

    /**
     * Analyze validated class and store metadata for IR generation.
     *
     * Similar to interface analysis but separates:
     * - Abstract properties/methods (must be implemented)
     * - Open properties/methods (can be overridden)
     *
     * @param declaration Validated FIR class declaration
     * @param session FIR session for type resolution
     */
    private fun analyzeAndStoreMetadata(
        declaration: FirClass,
        session: org.jetbrains.kotlin.fir.FirSession,
    ) {
        val classId = declaration.classId
        val simpleName = classId.shortClassName.asString()
        val packageName = classId.packageFqName.asString()

        // Phase 3C.1: Extract type parameters (same as interface)
        val typeParameters = extractTypeParameters(declaration)

        // Phase 3C.1: Extract abstract and open members separately
        val (abstractProps, openProps) = extractProperties(declaration)
        val (abstractMethods, openMethods) = extractMethods(declaration)

        // Create and store validated metadata
        val metadata =
            ValidatedFakeClass(
                classId = classId,
                simpleName = simpleName,
                packageName = packageName,
                typeParameters = typeParameters,
                abstractProperties = abstractProps,
                openProperties = openProps,
                abstractMethods = abstractMethods,
                openMethods = openMethods,
                sourceLocation = FirSourceLocation.UNKNOWN, // Phase 3D: source location extraction
            )

        sharedContext.metadataStorage.storeClass(metadata)
    }

    /**
     * Extract type parameters from FIR class declaration (Phase 3C.1).
     *
     * Same pattern as FakeInterfaceChecker - extracts type parameter names and bounds.
     *
     * @param declaration FIR class declaration
     * @return List of type parameter metadata
     */
    @OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
    private fun extractTypeParameters(declaration: FirClass): List<FirTypeParameterInfo> =
        declaration.typeParameters.map { typeParamRef ->
            val typeParam = typeParamRef.symbol.fir
            val name = typeParam.name.asString()

            // Extract type parameter bounds (e.g., T : Comparable<T>)
            val bounds =
                typeParam.bounds.map { boundRef ->
                    boundRef.coneType.toString()
                }

            FirTypeParameterInfo(
                name = name,
                bounds = bounds,
            )
        }

    /**
     * Extract properties from FIR class, separating abstract and open (Phase 3C.1).
     *
     * Returns pair of (abstract properties, open properties).
     * Uses modality to distinguish:
     * - ABSTRACT: Must be implemented by fake
     * - OPEN: Can be overridden (optional)
     *
     * @param declaration FIR class declaration
     * @return Pair of (abstract properties, open properties)
     */
    @OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
    private fun extractProperties(declaration: FirClass): Pair<List<FirPropertyInfo>, List<FirPropertyInfo>> {
        val abstractProperties = mutableListOf<FirPropertyInfo>()
        val openProperties = mutableListOf<FirPropertyInfo>()

        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            if (symbol is FirPropertySymbol) {
                val property = symbol.fir
                val name = property.name.asString()
                val type = property.returnTypeRef.coneType.toString()
                val isMutable = property.isVar
                val isNullable = property.returnTypeRef.coneType.isMarkedNullable

                val propertyInfo =
                    FirPropertyInfo(
                        name = name,
                        type = type,
                        isMutable = isMutable,
                        isNullable = isNullable,
                    )

                // Distinguish abstract vs open using modality
                when (property.modality) {
                    Modality.ABSTRACT -> abstractProperties.add(propertyInfo)
                    Modality.OPEN -> openProperties.add(propertyInfo)
                    else -> {
                        // FINAL or SEALED properties shouldn't appear in abstract classes,
                        // but skip them if they do
                    }
                }
            }
        }

        return Pair(abstractProperties, openProperties)
    }

    /**
     * Extract methods from FIR class, separating abstract and open (Phase 3C.1).
     *
     * Returns pair of (abstract methods, open methods).
     * Uses modality to distinguish:
     * - ABSTRACT: Must be implemented by fake
     * - OPEN: Can be overridden (call super or override)
     *
     * @param declaration FIR class declaration
     * @return Pair of (abstract methods, open methods)
     */
    @OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
    private fun extractMethods(declaration: FirClass): Pair<List<FirFunctionInfo>, List<FirFunctionInfo>> {
        val abstractMethods = mutableListOf<FirFunctionInfo>()
        val openMethods = mutableListOf<FirFunctionInfo>()

        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            if (symbol is FirNamedFunctionSymbol) {
                val function = symbol.fir
                val name = function.name.asString()

                // Extract parameters
                // Phase 3C.4: Extract default value expressions and render to code strings
                val parameters =
                    function.valueParameters.map { param ->
                        val defaultValue = param.defaultValue
                        val defaultValueCode =
                            if (defaultValue != null) {
                                renderDefaultValue(defaultValue) // null if rendering failed/not supported
                            } else {
                                null
                            }

                        FirParameterInfo(
                            name = param.name.asString(),
                            type = param.returnTypeRef.coneType.toString(),
                            hasDefaultValue = param.defaultValue != null,
                            defaultValueCode = defaultValueCode,
                            isVararg = param.isVararg,
                        )
                    }

                val returnType = function.returnTypeRef.coneType.toString()
                val isSuspend = function.isSuspend
                val isInline = function.isInline

                // Extract function-level type parameters with bounds
                val typeParameters =
                    function.typeParameters.map { typeParamRef ->
                        val typeParam = typeParamRef.symbol.fir
                        val bounds =
                            typeParam.bounds.map { boundRef ->
                                boundRef.coneType.toString()
                            }
                        FirTypeParameterInfo(
                            name = typeParam.name.asString(),
                            bounds = bounds,
                        )
                    }

                // Build typeParameterBounds map
                val typeParameterBounds =
                    typeParameters.associate { typeParam ->
                        typeParam.name to typeParam.bounds.firstOrNull().orEmpty()
                    }

                val functionInfo =
                    FirFunctionInfo(
                        name = name,
                        parameters = parameters,
                        returnType = returnType,
                        isSuspend = isSuspend,
                        isInline = isInline,
                        typeParameters = typeParameters,
                        typeParameterBounds = typeParameterBounds,
                    )

                // Distinguish abstract vs open using modality
                when (function.modality) {
                    Modality.ABSTRACT -> abstractMethods.add(functionInfo)
                    Modality.OPEN -> openMethods.add(functionInfo)
                    else -> {
                        // FINAL or SEALED methods shouldn't be fakeable,
                        // but skip them if they appear
                    }
                }
            }
        }

        return Pair(abstractMethods, openMethods)
    }

    /**
     * Report compilation error (Phase 3B.4 - Simplified).
     *
     * **Note**: FIR-level error reporting requires complex diagnostic factory setup
     * that varies by Kotlin version. For Phase 3B.4, we use simpler error logging
     * that ensures validation stops invalid declarations from being processed.
     *
     * The key goal: Detect and reject invalid @Fake usage early in FIR phase.
     * Full diagnostic integration can be added in future phases if needed.
     *
     * @param session FIR session
     * @param source Source element for context
     * @param message Error message to display
     */
    private fun reportError(
        session: org.jetbrains.kotlin.fir.FirSession,
        source: Any?,
        message: String,
    ) {
        // Phase 3B.4: Log error to stderr (visible during compilation)
        // This ensures developers see validation errors immediately
        System.err.println("ERROR: $message")

        // Note: The key behavior is that we return early from check(),
        // preventing invalid declarations from being stored in metadata.
        // This stops fake generation for invalid interfaces/classes.
    }
}
