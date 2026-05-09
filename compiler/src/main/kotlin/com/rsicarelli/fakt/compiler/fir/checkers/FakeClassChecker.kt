// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.checkers

import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.telemetry.measureTimeNanos
import com.rsicarelli.fakt.compiler.fir.extraction.AnnotationExtractor
import com.rsicarelli.fakt.compiler.fir.metadata.FirCallHistoryMode
import com.rsicarelli.fakt.compiler.fir.metadata.FirFunctionInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirMutabilityMode
import com.rsicarelli.fakt.compiler.fir.metadata.FirParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirPropertyInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirSourceLocation
import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.rendering.renderDefaultValue
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
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
 * **Note**: Class faking is secondary to interface faking in Fakt. The primary use case is
 * interfaces, but we support abstract classes for compatibility with existing code patterns.
 *
 * @property sharedContext Shared context with metadata storage and logger
 */
internal class FakeClassChecker(private val sharedContext: FaktSharedContext) :
    FirClassChecker(MppCheckerKind.Common) {
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

        val timedResult = measureTimeNanos { analyzeMetadata(declaration, session, simpleName) }

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
     * Open classes without open members cannot be faked because there's nothing to override.
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

                is FirFunctionSymbol<*> -> {
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
     * @param session FIR session for resolving the containing file
     * @param simpleName Simple name for logging
     * @return Validated metadata (timing will be added by caller)
     */
    private fun analyzeMetadata(
        declaration: FirClass,
        session: FirSession,
        simpleName: String,
    ): ValidatedFakeClass {
        val classId = declaration.classId
        val packageName = classId.packageFqName.asString()
        val qualifiedSourceName = classId.relativeClassName.asString()

        // Extract type parameters (same as interface)
        val typeParameters = extractTypeParameters(declaration)

        // Extract abstract and open members separately
        val (abstractProps, openProps) = extractProperties(declaration)
        val (abstractMethods, openMethods) = extractMethods(declaration)

        // Extract source location for KMP source set detection
        val sourceLocation = extractSourceLocation(declaration, session)

        // Extract visibility for explicitApi() support
        val visibility = FirVisibility.from(declaration.status.visibility)

        // Extract annotations (excluding @Fake itself)
        val annotations = AnnotationExtractor.extractAnnotations(declaration, session)

        // Extract call history mode from @Fake annotation
        val callHistoryMode = extractCallHistoryMode(declaration, session)

        // Extract mutability mode from @Fake annotation
        val mutabilityMode = extractMutabilityMode(declaration, session)

        // Create validated metadata (timing will be added by caller)
        return ValidatedFakeClass(
            classId = classId,
            simpleName = simpleName,
            packageName = packageName,
            qualifiedSourceName = qualifiedSourceName,
            typeParameters = typeParameters,
            abstractProperties = abstractProps,
            openProperties = openProps,
            abstractMethods = abstractMethods,
            openMethods = openMethods,
            annotations = annotations,
            sourceLocation = sourceLocation,
            validationTimeNanos = 0L, // Will be set by caller after timing measurement
            sourceSourceSet = sourceLocation.extractSourceSetName(),
            visibility = visibility,
            callHistoryMode = callHistoryMode,
            mutabilityMode = mutabilityMode,
        )
    }

    /**
     * Extract callHistory attribute from @Fake annotation.
     *
     * Reads the `callHistory` parameter from the @Fake annotation:
     * - `@Fake` → DEFAULT (no parameter specified)
     * - `@Fake(callHistory = CallHistoryMode.ENABLED)` → ENABLED
     * - `@Fake(callHistory = CallHistoryMode.DISABLED)` → DISABLED
     *
     * @param declaration FIR class declaration with @Fake annotation
     * @param session FIR session for annotation resolution
     * @return The extracted call history mode, or DEFAULT if not specified
     */
    private fun extractCallHistoryMode(
        declaration: FirClass,
        session: FirSession,
    ): FirCallHistoryMode {
        // Find the @Fake annotation
        val fakeAnnotation =
            declaration.annotations.find { annotation ->
                annotation.toAnnotationClassIdSafe(session) == FAKE_ANNOTATION_CLASS_ID
            } ?: return FirCallHistoryMode.DEFAULT

        // Look for the callHistory argument in the annotation
        val callHistoryArg =
            fakeAnnotation.argumentMapping.mapping.entries
                .find { it.key.asString() == "callHistory" }
                ?.value

        // Parse the enum value from the expression
        return when (callHistoryArg) {
            is FirPropertyAccessExpression -> {
                // Resolve the enum entry symbol
                val enumEntry = callHistoryArg.calleeReference.toResolvedEnumEntrySymbol()
                when (enumEntry?.name?.asString()) {
                    "ENABLED" -> FirCallHistoryMode.ENABLED
                    "DISABLED" -> FirCallHistoryMode.DISABLED
                    else -> FirCallHistoryMode.DEFAULT
                }
            }
            else -> FirCallHistoryMode.DEFAULT
        }
    }

    /**
     * Extract mutability attribute from @Fake annotation.
     *
     * Reads the `mutability` parameter from the @Fake annotation:
     * - `@Fake` → DEFAULT (no parameter specified)
     * - `@Fake(mutability = MutabilityMode.MUTABLE)` → MUTABLE
     * - `@Fake(mutability = MutabilityMode.IMMUTABLE)` → IMMUTABLE
     *
     * @param declaration FIR class declaration with @Fake annotation
     * @param session FIR session for annotation resolution
     * @return The extracted mutability mode, or DEFAULT if not specified
     */
    private fun extractMutabilityMode(
        declaration: FirClass,
        session: FirSession,
    ): FirMutabilityMode {
        val fakeAnnotation =
            declaration.annotations.find { annotation ->
                annotation.toAnnotationClassIdSafe(session) == FAKE_ANNOTATION_CLASS_ID
            } ?: return FirMutabilityMode.DEFAULT

        val mutabilityArg =
            fakeAnnotation.argumentMapping.mapping.entries
                .find { it.key.asString() == "mutability" }
                ?.value

        return when (mutabilityArg) {
            is FirPropertyAccessExpression -> {
                val enumEntry = mutabilityArg.calleeReference.toResolvedEnumEntrySymbol()
                when (enumEntry?.name?.asString()) {
                    "MUTABLE" -> FirMutabilityMode.MUTABLE
                    "IMMUTABLE" -> FirMutabilityMode.IMMUTABLE
                    else -> FirMutabilityMode.DEFAULT
                }
            }
            else -> FirMutabilityMode.DEFAULT
        }
    }

    /**
     * Extract source location from FIR class declaration.
     *
     * Source location is used for:
     * 1. Error messages (showing where the issue occurred)
     * 2. IDE navigation (click-through from errors)
     * 3. **KMP source set detection** (determining which test source set to output to)
     *
     * For KMP, the file path is parsed to determine the source set (e.g., "commonMain", "iosMain")
     * which then maps to the corresponding test source set for output.
     *
     * @param declaration FIR class declaration to extract source from
     * @param session FIR session for resolving the containing file
     * @return Source location metadata with file path for source set detection
     */
    private fun extractSourceLocation(
        declaration: FirClass,
        session: FirSession,
    ): FirSourceLocation {
        // Get the containing FirFile using firProvider
        // This is the proper K2 way to access file information from a FirClass
        val filePath =
            try {
                val firFile =
                    session.firProvider.getFirClassifierContainerFileIfAny(declaration.symbol)
                // KtSourceFile.path gives us the full file path
                firFile?.sourceFile?.path ?: "<unknown>"
            } catch (_: Exception) {
                "<unknown>"
            }

        return FirSourceLocation(
            filePath = filePath,
            startLine = 0,
            startColumn = 0,
            endLine = 0,
            endColumn = 0,
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
            val bounds = typeParam.bounds.map { boundRef -> boundRef.coneType.toString() }

            FirTypeParameterInfo(name = name, bounds = bounds)
        }

    /**
     * Extract properties from FIR class, separating abstract and open
     *
     * Returns pair of (abstract properties, open properties). Uses modality to distinguish:
     * - ABSTRACT: Must be implemented by fake
     * - OPEN: Can be overridden (optional)
     *
     * @param declaration FIR class declaration
     * @return Pair of (abstract properties, open properties)
     */
    @OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
    private fun extractProperties(
        declaration: FirClass
    ): Pair<List<FirPropertyInfo>, List<FirPropertyInfo>> {
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
     * Extract methods from FIR class, separating abstract and open.
     *
     * Returns pair of (abstract methods, open methods). Uses modality to distinguish:
     * - ABSTRACT: Must be implemented by fake
     * - OPEN: Can be overridden (call super or override)
     */
    @OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
    private fun extractMethods(
        declaration: FirClass
    ): Pair<List<FirFunctionInfo>, List<FirFunctionInfo>> {
        val abstractMethods = mutableListOf<FirFunctionInfo>()
        val openMethods = mutableListOf<FirFunctionInfo>()

        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            if (symbol is FirFunctionSymbol<*>) {
                val function = symbol.fir
                val functionInfo = extractFunctionInfo(function, symbol.name)

                when (function.modality) {
                    Modality.ABSTRACT -> abstractMethods.add(functionInfo)
                    Modality.OPEN -> openMethods.add(functionInfo)
                    else -> Unit // FINAL or SEALED methods are skipped
                }
            }
        }

        return abstractMethods to openMethods
    }

    /** Extract function metadata from FIR function declaration. */
    @OptIn(org.jetbrains.kotlin.fir.symbols.SymbolInternals::class)
    private fun extractFunctionInfo(
        function: FirFunction,
        functionName: org.jetbrains.kotlin.name.Name,
    ): FirFunctionInfo {
        val parameters = function.valueParameters.map(::extractParameterInfo)
        val typeParameters =
            function.typeParameters.map { typeParamRef ->
                val typeParam = typeParamRef.symbol.fir
                val bounds = typeParam.bounds.map { it.coneType.toString() }
                FirTypeParameterInfo(name = typeParam.name.asString(), bounds = bounds)
            }

        return FirFunctionInfo(
            name = functionName.asString(),
            parameters = parameters,
            returnType = function.returnTypeRef.coneType.toString(),
            isSuspend = function.isSuspend,
            isInline = function.isInline,
            typeParameters = typeParameters,
            typeParameterBounds =
                typeParameters.associate { it.name to it.bounds.firstOrNull().orEmpty() },
        )
    }

    /** Extract parameter metadata from FIR value parameter. */
    private fun extractParameterInfo(param: FirValueParameter): FirParameterInfo =
        FirParameterInfo(
            name = param.name.asString(),
            type = param.returnTypeRef.coneType.toString(),
            hasDefaultValue = param.defaultValue != null,
            defaultValueCode = param.defaultValue?.let(::renderDefaultValue),
            isVararg = param.isVararg,
        )
}
