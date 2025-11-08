// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir

import com.rsicarelli.fakt.compiler.fir.FirFunctionInfo
import com.rsicarelli.fakt.compiler.fir.FirPropertyInfo
import com.rsicarelli.fakt.compiler.fir.FirTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.ValidatedFakeInterface
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

/**
 * Transforms FIR metadata (string-based types) to IR generation metadata (IrTypes + IR nodes).
 *
 * **Phase 3B.3**: This is the critical component that eliminates redundant analysis.
 *
 * ## Transformation Flow
 * ```
 * FIR Phase (FakeInterfaceChecker)
 *   ↓ Extracts metadata (strings)
 * ValidatedFakeInterface
 *   ↓ FirToIrTransformer.transform()
 * IrGenerationMetadata (IrTypes + IR nodes)
 *   ↓
 * Code Generation (NO re-analysis!)
 * ```
 *
 * ## What This Does
 * 1. **Resolves Properties**: FirPropertyInfo (string) → IrPropertyMetadata (IrType + IrProperty node)
 * 2. **Resolves Functions**: FirFunctionInfo (string) → IrFunctionMetadata (IrType + IrSimpleFunction node)
 * 3. **Formats Type Parameters**: ["T", "K : Comparable<K>"]
 * 4. **Computes GenericPattern**: Uses GenericPatternAnalyzer (reuses existing logic)
 *
 * ## What This Does NOT Do
 * - ❌ NO structural analysis (FIR already did that)
 * - ❌ NO validation (FIR already validated)
 * - ❌ NO walking IrClass.declarations for discovery
 *
 * This is a **pure transformation** layer, not an analyzer.
 *
 * @property patternAnalyzer Reused analyzer for GenericPattern classification
 */
internal class FirToIrTransformer {
    private val patternAnalyzer = GenericPatternAnalyzer()

    /**
     * Transform ValidatedFakeInterface to IrGenerationMetadata.
     *
     * **Critical**: This method uses FIR metadata as the source of truth for structure.
     * IrClass is only used for:
     * 1. Looking up IR nodes (IrProperty, IrSimpleFunction) by name
     * 2. Resolving type representations (IrType from IR nodes)
     * 3. Computing GenericPattern (uses IrClass.typeParameters)
     *
     * NO structural discovery or validation happens here - FIR already did that.
     *
     * @param firMetadata Validated metadata from FIR phase (source of truth)
     * @param irClass IR class for node lookup and type resolution
     * @return IrGenerationMetadata ready for code generation
     */
    fun transform(
        firMetadata: ValidatedFakeInterface,
        irClass: IrClass,
    ): IrGenerationMetadata {
        // 1. Resolve directly declared properties (FIR strings → IrTypes + IR nodes)
        val declaredProperties =
            firMetadata.properties.map { firProperty ->
                resolveProperty(firProperty, irClass)
            }

        // 2. Resolve directly declared functions (FIR strings → IrTypes + IR nodes)
        val declaredFunctions =
            firMetadata.functions.map { firFunction ->
                resolveFunction(firFunction, irClass)
            }

        // Phase 3C.3: Resolve inherited members
        // Inherited members need to be resolved against the IR class as well
        val inheritedProperties =
            firMetadata.inheritedProperties.map { firProperty ->
                resolveProperty(firProperty, irClass)
            }

        val inheritedFunctions =
            firMetadata.inheritedFunctions.map { firFunction ->
                resolveFunction(firFunction, irClass)
            }

        // Combine declared and inherited members for code generation
        // The fake implementation needs to provide implementations for ALL members
        val allProperties = declaredProperties + inheritedProperties
        val allFunctions = declaredFunctions + inheritedFunctions

        // 3. Format type parameters with bounds
        val typeParameters = firMetadata.typeParameters.map { formatTypeParameter(it) }

        // 4. Compute generic pattern (reuses existing GenericPatternAnalyzer)
        val genericPattern = patternAnalyzer.analyzeInterface(irClass)

        return IrGenerationMetadata(
            interfaceName = firMetadata.simpleName,
            packageName = firMetadata.packageName,
            typeParameters = typeParameters,
            properties = allProperties,
            functions = allFunctions,
            genericPattern = genericPattern,
            sourceInterface = irClass,
        )
    }

    /**
     * Transform ValidatedFakeClass to IrClassGenerationMetadata.
     *
     * Similar to transform() but handles abstract classes with separate abstract/open members.
     *
     * **Phase 3C.1**: Added to support abstract class fake generation.
     *
     * Key differences from interface transformation:
     * - Separates abstract properties/methods (must implement)
     * - Separates open properties/methods (can override)
     * - Both lists need fake implementations (Phase 3C.1 treats them the same)
     *
     * @param firMetadata Validated class metadata from FIR phase
     * @param irClass IR class for node lookup and type resolution
     * @return IrClassGenerationMetadata ready for code generation
     */
    fun transformClass(
        firMetadata: ValidatedFakeClass,
        irClass: IrClass,
    ): IrClassGenerationMetadata {
        // 1. Resolve abstract properties
        val abstractProperties =
            firMetadata.abstractProperties.map { firProperty ->
                resolveProperty(firProperty, irClass)
            }

        // 2. Resolve open properties
        val openProperties =
            firMetadata.openProperties.map { firProperty ->
                resolveProperty(firProperty, irClass)
            }

        // 3. Resolve abstract methods
        val abstractMethods =
            firMetadata.abstractMethods.map { firFunction ->
                resolveFunction(firFunction, irClass)
            }

        // 4. Resolve open methods
        val openMethods =
            firMetadata.openMethods.map { firFunction ->
                resolveFunction(firFunction, irClass)
            }

        // 5. Format type parameters with bounds
        val typeParameters = firMetadata.typeParameters.map { formatTypeParameter(it) }

        // 6. Compute generic pattern (reuses existing GenericPatternAnalyzer)
        val genericPattern = patternAnalyzer.analyzeInterface(irClass)

        return IrClassGenerationMetadata(
            className = firMetadata.simpleName,
            packageName = firMetadata.packageName,
            typeParameters = typeParameters,
            abstractProperties = abstractProperties,
            openProperties = openProperties,
            abstractMethods = abstractMethods,
            openMethods = openMethods,
            genericPattern = genericPattern,
            sourceClass = irClass,
        )
    }

    /**
     * Resolve FirPropertyInfo to IrPropertyMetadata.
     *
     * Lookup process:
     * 1. Find IrProperty by name in IrClass.declarations
     * 2. Extract IrType from getter.returnType or backingField.type
     * 3. Preserve mutability/nullability from FIR metadata
     *
     * @param firProperty FIR property metadata (source of truth for structure)
     * @param irClass IR class for node lookup
     * @return IrPropertyMetadata with resolved IrType and IR node
     */
    private fun resolveProperty(
        firProperty: FirPropertyInfo,
        irClass: IrClass,
    ): IrPropertyMetadata {
        // Lookup IrProperty by name (FIR guarantees this exists)
        val irProperty =
            irClass.declarations
                .filterIsInstance<IrProperty>()
                .firstOrNull { it.name.asString() == firProperty.name }
                ?: error(
                    "Phase 3B.3: IrProperty '${firProperty.name}' not found in ${irClass.name}. " +
                        "FIR validation should have ensured this exists.",
                )

        // Resolve IrType from IR node
        val resolvedType =
            irProperty.getter?.returnType
                ?: irProperty.backingField?.type
                ?: error(
                    "Phase 3B.3: IrProperty '${firProperty.name}' has no type. " +
                        "This should not happen for valid properties.",
                )

        return IrPropertyMetadata(
            name = firProperty.name,
            type = resolvedType,
            isMutable = firProperty.isMutable,
            isNullable = firProperty.isNullable,
            irProperty = irProperty,
        )
    }

    /**
     * Resolve FirFunctionInfo to IrFunctionMetadata.
     *
     * Lookup process:
     * 1. Find IrSimpleFunction by name in IrClass.declarations
     * 2. Extract IrType for return type
     * 3. Resolve parameters (match by position, FIR guarantees same order)
     * 4. Preserve suspend/inline flags from FIR metadata
     *
     * @param firFunction FIR function metadata (source of truth for structure)
     * @param irClass IR class for node lookup
     * @return IrFunctionMetadata with resolved IrTypes and IR node
     */
    private fun resolveFunction(
        firFunction: FirFunctionInfo,
        irClass: IrClass,
    ): IrFunctionMetadata {
        // Lookup IrSimpleFunction by name (FIR guarantees this exists)
        val irFunction =
            irClass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .firstOrNull { it.name.asString() == firFunction.name }
                ?: error(
                    "Phase 3B.3: IrSimpleFunction '${firFunction.name}' not found in ${irClass.name}. " +
                        "FIR validation should have ensured this exists.",
                )

        // Resolve parameters (match by position - FIR guarantees same order)
        val irRegularParams = irFunction.parameters.filter { it.kind == IrParameterKind.Regular }

        if (irRegularParams.size != firFunction.parameters.size) {
            error(
                "Phase 3B.3: Parameter count mismatch for '${firFunction.name}'. " +
                    "FIR: ${firFunction.parameters.size}, IR: ${irRegularParams.size}",
            )
        }

        val parameters =
            firFunction.parameters.zip(irRegularParams).map { (firParam, irParam) ->
                IrParameterMetadata(
                    name = firParam.name,
                    type = irParam.type,
                    hasDefaultValue = firParam.hasDefaultValue,
                    defaultValueCode = firParam.defaultValueCode, // Phase 3C.4: Pass through default value code
                    isVararg = firParam.isVararg,
                )
            }

        // Format method-level type parameters
        val methodTypeParameters = firFunction.typeParameters.map { formatTypeParameter(it) }

        return IrFunctionMetadata(
            name = firFunction.name,
            parameters = parameters,
            returnType = irFunction.returnType,
            isSuspend = firFunction.isSuspend,
            isInline = firFunction.isInline,
            typeParameters = methodTypeParameters,
            typeParameterBounds = firFunction.typeParameterBounds,
            irFunction = irFunction,
        )
    }

    /**
     * Format type parameter with bounds.
     *
     * Examples:
     * - `T` with no bounds → "T"
     * - `K` with bounds ["Comparable<K>"] → "K : Comparable<K>"
     * - `V` with bounds ["Comparable<V>", "Serializable"] → "V : Comparable<V>, Serializable"
     *
     * **Phase 3C.2**: Sanitizes type bounds from FIR to fix kotlin/ prefix issue.
     * FIR's coneType.toString() produces "kotlin/Any?" which is invalid Kotlin syntax.
     * This method converts it to proper dotted notation "kotlin.Any?" and then simplifies
     * kotlin stdlib types to just their simple name (e.g., "Any?" instead of "kotlin.Any?").
     *
     * @param firTypeParam FIR type parameter with bounds
     * @return Formatted string for code generation
     */
    private fun formatTypeParameter(firTypeParam: FirTypeParameterInfo): String {
        if (firTypeParam.bounds.isEmpty()) {
            return firTypeParam.name
        }
        // Sanitize bounds: kotlin/Foo -> kotlin.Foo -> Foo (for kotlin.* types)
        val sanitizedBounds = firTypeParam.bounds.map { bound -> sanitizeTypeBound(bound) }
        return "${firTypeParam.name} : ${sanitizedBounds.joinToString(", ")}"
    }

    /**
     * Sanitize type bound string from FIR phase.
     *
     * FIR's ConeType.toString() produces paths with slashes (e.g., "kotlin/Any?", "kotlin/Comparable<T>")
     * which is invalid Kotlin syntax. This method converts them to proper dotted notation
     * and simplifies kotlin stdlib types.
     *
     * Examples:
     * - "kotlin/Any?" → "Any?"
     * - "kotlin/Comparable<T>" → "Comparable<T>"
     * - "kotlin/collections/List<T>" → "List<T>"
     * - "com/example/CustomType" → "com.example.CustomType"
     *
     * @param bound Raw bound string from FIR (may contain slashes)
     * @return Sanitized bound string with proper Kotlin syntax
     */
    private fun sanitizeTypeBound(bound: String): String {
        // Step 1: Replace forward slashes with dots for package notation
        val dotted = bound.replace('/', '.')

        // Step 2: Remove kotlin. prefix for stdlib types (cleaner generated code)
        // Match kotlin.Foo or kotlin.collections.Foo but preserve the rest
        return when {
            // kotlin.collections.Foo -> Foo
            dotted.startsWith("kotlin.collections.") -> dotted.removePrefix("kotlin.collections.")
            // kotlin.Foo -> Foo
            dotted.startsWith("kotlin.") -> dotted.removePrefix("kotlin.")
            // Other packages remain unchanged
            else -> dotted
        }
    }
}
