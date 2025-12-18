// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.checkers

import com.rsicarelli.fakt.compiler.api.TimeFormatter
import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.telemetry.measureTimeNanos
import com.rsicarelli.fakt.compiler.fir.metadata.FirFunctionInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirPropertyInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirSourceLocation
import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.rendering.renderDefaultValue
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
 * @property sharedContext Shared context with metadata storage and logger
 */
internal class FakeClassChecker(
    private val sharedContext: FaktSharedContext,
) : FirClassChecker(MppCheckerKind.Common) {
    // Extract logger from shared context for performance tracking and debugging
    private val logger = sharedContext.logger

    companion object {
        // @Fake annotation ClassId
        private val FAKE_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("com.rsicarelli.fakt.Fake"))
    }

    // Validation logic: early returns are idiomatic guard clauses
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val session = context.session
        val classId = declaration.classId
        val simpleName = classId.shortClassName.asString()

        // Check if class has @Fake annotation
        if (!declaration.hasAnnotation(FAKE_ANNOTATION_CLASS_ID, session)) return

        // Skip if already validated as interface (FakeInterfaceChecker handles it)
        if (declaration.classKind == ClassKind.INTERFACE) {
            return
        }

        // KMP optimization: Try to load cached metadata from metadata compilation
        // If cache is valid and loaded, skip FIR analysis (data already in storage)
        if (sharedContext.cacheManager.tryLoadCache(sharedContext.metadataStorage)) {
            return // Cache hit - skip FIR analysis (count tracked in MetadataCacheManager)
        }

        // Validate it's a class
        if (declaration.classKind != ClassKind.CLASS) {
            logger.debug("Skipped $simpleName: not a class (classKind=${declaration.classKind})")
            logger.error(FirFaktErrors.FAKE_CLASS_MUST_BE_ABSTRACT)
            return // Skip non-classes
        }

        // Validate class modality (abstract or open)
        when (declaration.modality) {
            Modality.ABSTRACT -> {
                // Allow abstract classes
            }

            Modality.OPEN -> {
                // Allow open classes if they have open members
                if (!hasOpenMembers(declaration)) {
                    logger.debug("Skipped $simpleName: open class with no open members")
                    logger.error(FirFaktErrors.FAKE_OPEN_CLASS_NO_OPEN_MEMBERS)
                    return
                }
            }

            Modality.FINAL -> {
                logger.debug("Skipped $simpleName: final class not supported")
                logger.error(FirFaktErrors.FAKE_CLASS_CANNOT_BE_FINAL)
                return
            }

            Modality.SEALED -> {
                logger.debug("Skipped $simpleName: sealed class not supported")
                logger.error(FirFaktErrors.FAKE_CLASS_CANNOT_BE_SEALED)
                return
            }

            null -> {
                logger.debug("Skipped $simpleName: null modality (treated as final)")
                logger.error(FirFaktErrors.FAKE_CLASS_CANNOT_BE_FINAL)
                return
            }
        }

        // Validate not local
        if (declaration.symbol.classId.isLocal) {
            logger.debug("Skipped $simpleName: local class not supported")
            logger.error(FirFaktErrors.FAKE_CANNOT_BE_LOCAL)
            return
        }

        // Validate not expect (KMP multiplatform)
        if (declaration.status.isExpect) {
            logger.debug("Skipped $simpleName: expect class not supported")
            logger.error(FirFaktErrors.FAKE_CANNOT_BE_EXPECT)
            return
        }

        // Validate not external
        if (declaration.status.isExternal) {
            logger.debug("Skipped $simpleName: external class not supported")
            logger.error(FirFaktErrors.FAKE_CANNOT_BE_EXTERNAL)
            return
        }

        val timedResult =
            measureTimeNanos {
                analyzeMetadata(declaration, simpleName)
            }

        // Store metadata with validation timing for consolidated logging in IR phase
        val metadataWithTiming =
            timedResult.result.copy(validationTimeNanos = timedResult.durationNanos)
        sharedContext.metadataStorage.storeClass(metadataWithTiming)

        // Write cache for producer mode (metadata compilation)
        // This is called after each class to ensure cache is written even if IR phase doesn't run
        // (metadata compilation doesn't have IR phase)
        // Note: Don't log here - writeCache logs the summary on the final write
        if (sharedContext.cacheManager.isProducerMode) {
            sharedContext.cacheManager.writeCache(sharedContext.metadataStorage)
        }
    }

    /**
     * Check if class has any open members (properties or methods).
     *
     * Open classes without open members cannot be faked
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
                is FirPropertySymbol -> {
                    if (symbol.fir.modality == Modality.OPEN) {
                        hasOpen = true
                    }
                }

                is FirNamedFunctionSymbol -> {
                    if (symbol.fir.modality == Modality.OPEN) {
                        hasOpen = true
                    }
                }
            }
        }

        return hasOpen
    }

    /**
     * Analyze validated class and create metadata for IR generation.
     *
     * Similar to interface analysis but separates:
     * - Abstract properties/methods (must be implemented)
     * - Open properties/methods (can be overridden)
     *
     * Note: Validation timing is added by caller after this returns.
     *
     * @param declaration Validated FIR class declaration
     * @param simpleName Simple name for logging
     * @return Validated metadata (timing will be added by caller)
     */
    private fun analyzeMetadata(
        declaration: FirClass,
        simpleName: String,
    ): ValidatedFakeClass {
        val classId = declaration.classId
        val packageName = classId.packageFqName.asString()

        // Extract type parameters (same as interface)
        val typeParameters = extractTypeParameters(declaration)

        // Extract abstract and open members separately
        val (abstractProps, openProps) = extractProperties(declaration)
        val (abstractMethods, openMethods) = extractMethods(declaration)

        // Create validated metadata (timing will be added by caller)
        return ValidatedFakeClass(
            classId = classId,
            simpleName = simpleName,
            packageName = packageName,
            typeParameters = typeParameters,
            abstractProperties = abstractProps,
            openProperties = openProps,
            abstractMethods = abstractMethods,
            openMethods = openMethods,
            sourceLocation = FirSourceLocation.UNKNOWN, // source location extraction
            validationTimeNanos = 0L, // Will be set by caller after timing measurement
        )
    }

    /**
     * Extract type parameters from FIR class declaration
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
     * Extract properties from FIR class, separating abstract and open
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
     * Extract methods from FIR class, separating abstract and open
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
                // Extract default value expressions and render to code strings
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
}
