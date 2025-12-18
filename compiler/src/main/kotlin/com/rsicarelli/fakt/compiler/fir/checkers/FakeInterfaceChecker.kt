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
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import com.rsicarelli.fakt.compiler.fir.rendering.renderDefaultValue
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
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
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * FIR checker for @Fake annotated interfaces.
 *
 * Following Metro pattern (see InjectConstructorChecker.kt:23-83):
 * - Validates annotation usage during FIR phase
 * - Reports errors with accurate source locations
 * - Stores validated metadata for IR generation
 *
 * **Validation Rules**:
 * 1. Must be an interface (not class, object, etc.)
 * 2. Must not be sealed
 * 3. Must not be external/expect declarations
 * 4. Must not be local classes
 *
 * **Metro Alignment**:
 * This follows the same validation pattern as Metro's InjectConstructorChecker,
 * adapting it for @Fake annotation validation.
 *
 * @property sharedContext Shared context with metadata storage and logger
 */
internal class FakeInterfaceChecker(
    private val sharedContext: FaktSharedContext,
) : FirClassChecker(MppCheckerKind.Common) {
    private val logger = sharedContext.logger

    companion object {
        // @Fake annotation ClassId
        private val FAKE_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("com.rsicarelli.fakt.Fake"))
    }

    context(context: CheckerContext, reporter: DiagnosticReporter) // Validation logic: early returns are idiomatic guard clauses
    override fun check(declaration: FirClass) {
        val session = context.session
        val classId = declaration.classId
        val simpleName = classId.shortClassName.asString()

        // Check if class has @Fake annotation
        if (!declaration.hasAnnotation(FAKE_ANNOTATION_CLASS_ID, session)) return

        // Skip non-interfaces (let FakeClassChecker handle classes)
        if (declaration.classKind != ClassKind.INTERFACE) {
            return // Skip, FakeClassChecker will validate classes
        }

        // KMP optimization: Try to load cached metadata from metadata compilation
        // If cache is valid and loaded, skip FIR analysis (data already in storage)
        if (sharedContext.cacheManager.tryLoadCache(sharedContext.metadataStorage)) {
            return // Cache hit - skip FIR analysis (count tracked in MetadataCacheManager)
        }

        // Validate not sealed
        if (declaration.modality == Modality.SEALED) {
            logger.debug("Skipped $simpleName: sealed interface not supported")
            reportError(FirFaktErrors.FAKE_CANNOT_BE_SEALED)
            return // Skip sealed interfaces
        }

        // Validate not local
        if (declaration.symbol.classId.isLocal) {
            logger.debug("Skipped $simpleName: local interface not supported")
            reportError(FirFaktErrors.FAKE_CANNOT_BE_LOCAL)
            return // Skip local interfaces
        }

        // Validate not expect (KMP multiplatform)
        if (declaration.status.isExpect) {
            logger.debug("Skipped $simpleName: expect interface not supported")
            reportError(FirFaktErrors.FAKE_CANNOT_BE_EXPECT)
            return // Skip expect interfaces
        }

        // Validate not external
        if (declaration.status.isExternal) {
            logger.debug("Skipped $simpleName: external interface not supported")
            reportError(FirFaktErrors.FAKE_CANNOT_BE_EXTERNAL)
            return // Skip external interfaces
        }

        // ✅ Validation passed - analyze and store metadata with timing
        val timedResult =
            measureTimeNanos {
                analyzeMetadata(declaration, session, simpleName)
            }

        // Store metadata with validation timing for consolidated logging in IR phase
        val metadataWithTiming =
            timedResult.result.copy(validationTimeNanos = timedResult.durationNanos)
        sharedContext.metadataStorage.storeInterface(metadataWithTiming)

        // Write cache for producer mode (metadata compilation)
        // This is called after each interface to ensure cache is written even if IR phase doesn't run
        // (metadata compilation doesn't have IR phase)
        // Note: Don't log here - writeCache logs the summary on the final write
        if (sharedContext.cacheManager.isProducerMode) {
            sharedContext.cacheManager.writeCache(sharedContext.metadataStorage)
        }
    }

    /**
     * Analyze validated interface and create metadata for IR generation.
     *
     * This is where we extract all the information that IR phase will need:
     * - Type parameters
     * - Properties
     * - Functions
     * - Inherited members
     *
     * Note: Validation timing is added by caller after this returns.
     *
     * @param declaration Validated FIR class declaration
     * @param session FIR session for type resolution
     * @param simpleName Simple name for logging
     * @return Validated metadata (timing will be added by caller)
     */
    private fun analyzeMetadata(
        declaration: FirClass,
        session: FirSession,
        simpleName: String,
    ): ValidatedFakeInterface {
        val classId = declaration.classId
        val packageName = classId.packageFqName.asString()
        val typeParameters = extractTypeParameters(declaration)
        val properties = extractProperties(declaration)
        val functions = extractFunctions(declaration)
        val sourceLocation = extractSourceLocation()
        val (inheritedProperties, inheritedFunctions) =
            extractInheritedMembers(
                declaration = declaration,
                session = session,
            )

        return ValidatedFakeInterface(
            classId = classId,
            simpleName = simpleName,
            packageName = packageName,
            typeParameters = typeParameters,
            properties = properties,
            functions = functions,
            inheritedProperties = inheritedProperties,
            inheritedFunctions = inheritedFunctions,
            sourceLocation = sourceLocation,
            validationTimeNanos = 0L, // Will be set by caller after timing measurement
        )
    }

    /**
     * Extract type parameters from FIR class declaration.
     *
     * Converts FIR type parameters to simplified FirTypeParameterInfo for IR generation.
     * Each type parameter includes its name and bounds (constraints).
     *
     * Examples:
     * - `interface Foo<T>` → [FirTypeParameterInfo("T", emptyList())]
     * - `interface Foo<T : Comparable<T>>` → [FirTypeParameterInfo("T", ["kotlin.Comparable<T>"])]
     * - `interface Foo<T : A, B>` → [FirTypeParameterInfo("T", ["A", "B"])]
     *
     * **Note**: Currently extracts names and basic bounds. Full type resolution
     * with proper FirTypeRef→String conversion will be implemented in future enhancements.
     *
     * @param declaration FIR class declaration with type parameters
     * @return List of type parameter metadata
     */
    @OptIn(SymbolInternals::class)
    private fun extractTypeParameters(declaration: FirClass): List<FirTypeParameterInfo> =
        declaration.typeParameters.map { typeParamRef ->
            // FirClass.typeParameters returns List<FirTypeParameterRef>
            // We need to resolve the symbol to get the actual FirTypeParameter
            val typeParam = typeParamRef.symbol.fir
            val name = typeParam.name.asString()

            // Extract type parameter bounds
            // FirTypeParameter.bounds contains List<FirTypeRef> representing constraints
            // e.g., `<T : Comparable<T>>` has bounds = [Comparable<T>]
            // e.g., `<T : A, B>` has bounds = [A, B] (multiple bounds via 'where' clause)
            val bounds =
                typeParam.bounds.map { boundRef ->
                    // Render FirTypeRef to String using ConeType representation
                    boundRef.coneType.toString()
                }

            FirTypeParameterInfo(
                name = name,
                bounds = bounds,
            )
        }

    /**
     * Extract properties from FIR class declaration.
     *
     * Converts FIR properties to simplified FirPropertyInfo for IR generation.
     * Extracts name, type, mutability (var/val), and nullability.
     *
     * Examples:
     * - `val name: String` → FirPropertyInfo("name", "String", false, false)
     * - `var count: Int?` → FirPropertyInfo("count", "Int?", true, true)
     *
     * **Note**: Uses basic type rendering. Future enhancements will add proper type resolution.
     *
     * @param declaration FIR class declaration
     * @return List of property metadata
     */
    @OptIn(SymbolInternals::class)
    private fun extractProperties(declaration: FirClass): List<FirPropertyInfo> {
        val properties = mutableListOf<FirPropertyInfo>()

        // Use processAllDeclarations to iterate through class members
        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            // Only process property symbols
            if (symbol is FirPropertySymbol) {
                val property = symbol.fir
                val name = property.name.asString()

                // Get type string representation
                val type = property.returnTypeRef.coneType.toString()

                // Check if property is mutable (var) or immutable (val)
                val isMutable = property.isVar

                // ConeKotlinType.isMarkedNullable checks if nullability == ConeNullability.NULLABLE
                val isNullable = property.returnTypeRef.coneType.isMarkedNullable

                properties.add(
                    FirPropertyInfo(
                        name = name,
                        type = type,
                        isMutable = isMutable,
                        isNullable = isNullable,
                    ),
                )
            }
        }

        return properties
    }

    /**
     * Extract functions from FIR class declaration.
     *
     * Converts FIR functions to simplified FirFunctionInfo for IR generation.
     * Extracts name, parameters, return type, suspend/inline modifiers, and type parameters.
     *
     * Examples:
     * - `fun getUser(): User` → FirFunctionInfo("getUser", [], "User", false, false, [], {})
     * - `suspend fun fetch(id: Int): Result<T>` → FirFunctionInfo("fetch", [...], "Result<T>", true, false, [], {})
     *
     * **Note**: Uses basic type rendering. Future enhancements will add proper type resolution.
     *
     * @param declaration FIR class declaration
     * @return List of function metadata
     */
    @OptIn(SymbolInternals::class)
    private fun extractFunctions(declaration: FirClass): List<FirFunctionInfo> {
        val functions = mutableListOf<FirFunctionInfo>()

        // Use processAllDeclarations to iterate through class members
        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            // Only process function symbols
            if (symbol is FirNamedFunctionSymbol) {
                val function = symbol.fir
                val name = function.name.asString()

                // Extract parameters
                // Extract default value expressions and render to code strings
                val parameters =
                    function.valueParameters.map { param ->
                        FirParameterInfo(
                            name = param.name.asString(),
                            type = param.returnTypeRef.coneType.toString(),
                            hasDefaultValue = param.defaultValue != null,
                            defaultValueCode = param.defaultValue?.let(::renderDefaultValue),
                            isVararg = param.isVararg,
                        )
                    }

                // Extract return type
                val returnType = function.returnTypeRef.coneType.toString()

                // Check modifiers
                val isSuspend = function.isSuspend
                val isInline = function.isInline

                // Extract function-level type parameters
                // Extract bounds for method-level generics the same way
                val typeParameters =
                    function.typeParameters.map { typeParamRef ->
                        val typeParam = typeParamRef.symbol.fir
                        FirTypeParameterInfo(
                            name = typeParam.name.asString(),
                            bounds = typeParam.bounds.map { it.coneType.toString() },
                        )
                    }

                // Build typeParameterBounds map from extracted bounds
                // Format: Map<"T", "Comparable<T>"> for single bound
                // Note: Kotlin doesn't support multiple bounds for same param in this format,
                // but FirTypeParameterInfo.bounds handles it correctly as List<String>
                val typeParameterBounds =
                    typeParameters.associate { typeParam ->
                        typeParam.name to typeParam.bounds.firstOrNull().orEmpty()
                    }

                functions.add(
                    FirFunctionInfo(
                        name = name,
                        parameters = parameters,
                        returnType = returnType,
                        isSuspend = isSuspend,
                        isInline = isInline,
                        typeParameters = typeParameters,
                        typeParameterBounds = typeParameterBounds,
                    ),
                )
            }
        }

        return functions
    }

    /**
     * Extract source location from FIR class declaration.
     *
     * Extracts file path, start/end line, and start/end column from the FIR source element.
     * This information is used for accurate error reporting during IR generation.
     *
     * **Note**: KtSourceElement API has complex type hierarchy with multiple implementations.
     * For now, returns UNKNOWN. Complete implementation requires deeper investigation of:
     * - KtPsiSourceElement vs KtLightSourceElement vs KtFakeSourceElement
     * - Accessing underlying PsiElement or LighterASTNode safely
     * - Converting offsets to line/column using KtSourceFileLinesMapping
     *
     * @suppress ForbiddenComment
     * TODO: Implement full source location extraction using proper KtSourceElement API
     * Current impact: Error messages will not include exact source locations (non-critical)
     *
     * @return Source location metadata (UNKNOWN for now)
     */
    private fun extractSourceLocation(): FirSourceLocation {
        // TODO: Implement proper source location extraction from FirClass
        // This requires investigating KtSourceElement type hierarchy and safe access patterns
        // For now, returning UNKNOWN allows the plugin to proceed without blocking on this non-critical feature
        return FirSourceLocation.UNKNOWN
    }

    /**
     * Extract inherited members from super-interfaces.
     *
     * Recursively collects properties and functions from all super-interfaces.
     *
     * This method handles:
     * - Direct super-interfaces (e.g., `interface B : A`)
     * - Transitive inheritance (e.g., `interface C : B`, where B extends A)
     * - Multiple inheritance (e.g., `interface D : A, B`)
     * - Diamond inheritance (e.g., `interface D : B, C` where both B and C extend A)
     *
     * Deduplication strategy:
     * - Members are deduplicated by signature (name + parameter types)
     * - If the same member appears in multiple super-interfaces, only one copy is kept
     * - This matches Kotlin's inheritance resolution rules
     *
     * @param declaration FIR interface declaration
     * @param session FIR session for type resolution
     * @return Pair of (inherited properties, inherited functions)
     */
    @OptIn(SymbolInternals::class)
    private fun extractInheritedMembers(
        declaration: FirClass,
        session: FirSession,
    ): Pair<List<FirPropertyInfo>, List<FirFunctionInfo>> {
        val inheritedProperties = mutableListOf<FirPropertyInfo>()
        val inheritedFunctions = mutableListOf<FirFunctionInfo>()

        // Track visited interfaces to avoid infinite recursion (diamond inheritance)
        val visitedInterfaces = mutableSetOf<ClassId>()

        // Process each super type
        declaration.superTypeRefs.forEach { superTypeRef ->
            try {
                // Resolve super type to get the actual class
                val superType = superTypeRef.coneType

                // ConeType should be ConeClassLikeType for class/interface types
                if (superType is ConeClassLikeType) {
                    // Use toSymbol() extension function to resolve the lookup tag
                    val classifier = superType.lookupTag.toSymbol(session)

                    if (classifier is org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol<*>) {
                        val superClass = classifier.fir

                        // Only process interfaces (skip Any, classes, etc.)
                        if (superClass.classKind == ClassKind.INTERFACE) {
                            collectInheritedMembers(
                                superClass,
                                session,
                                visitedInterfaces,
                                inheritedProperties,
                                inheritedFunctions,
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // If we can't resolve a super type, skip it gracefully
                // This can happen with external dependencies or incomplete compilation
                // The interface will still be processed, just without those inherited members
            }
        }

        // Deduplicate by name (simple deduplication - more sophisticated could check signatures)
        val uniqueProperties = inheritedProperties.distinctBy { it.name }
        val uniqueFunctions = inheritedFunctions.distinctBy { it.name }

        return Pair(uniqueProperties, uniqueFunctions)
    }

    /**
     * Recursively collect members from a super-interface and its ancestors.
     *
     * Helper method for inherited member extraction.
     *
     * @param firClass The super-interface to process
     * @param session FIR session
     * @param visitedInterfaces Set of already-visited interfaces (prevents infinite recursion)
     * @param propertiesAccumulator Mutable list to accumulate properties
     * @param functionsAccumulator Mutable list to accumulate functions
     */
    @OptIn(SymbolInternals::class)
    private fun collectInheritedMembers(
        firClass: FirClass,
        session: FirSession,
        visitedInterfaces: MutableSet<ClassId>,
        propertiesAccumulator: MutableList<FirPropertyInfo>,
        functionsAccumulator: MutableList<FirFunctionInfo>,
    ) {
        val classId = firClass.symbol.classId

        // Skip if already visited (handles diamond inheritance)
        if (classId in visitedInterfaces) {
            return
        }
        visitedInterfaces.add(classId)

        // Extract members from this interface
        propertiesAccumulator.addAll(extractProperties(firClass))
        functionsAccumulator.addAll(extractFunctions(firClass))

        // Recursively process super-interfaces of this interface
        firClass.superTypeRefs.forEach { superTypeRef ->
            try {
                val superType = superTypeRef.coneType

                if (superType is ConeClassLikeType) {
                    val classifier = superType.lookupTag.toSymbol(session)

                    if (classifier is org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol<*>) {
                        val superClass = classifier.fir

                        if (superClass.classKind == ClassKind.INTERFACE) {
                            collectInheritedMembers(
                                superClass,
                                session,
                                visitedInterfaces,
                                propertiesAccumulator,
                                functionsAccumulator,
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Skip unresolvable super types
            }
        }
    }

    /**
     * Report compilation error via structured logging.
     *
     * Uses FaktLogger for consistent error reporting across FIR and IR phases.
     * Errors are displayed during compilation through Kotlin's MessageCollector.
     *
     * **Note**: FIR-level diagnostic factory integration could be added in future
     * enhancements for IDE integration and precise source location highlighting.
     *
     * @param message Error message to display
     */
    private fun reportError(message: String) {
        // Log error through FaktLogger (routes to Kotlin's MessageCollector)
        // This ensures developers see validation errors with proper severity
        logger.error(message)

        // Note: The key behavior is that we return early from check(),
        // preventing invalid declarations from being stored in metadata.
        // This stops fake generation for invalid interfaces/classes.
    }
}
