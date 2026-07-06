// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.transform

import com.rsicarelli.fakt.compiler.core.types.TypeResolution
import com.rsicarelli.fakt.compiler.fir.metadata.FirAnnotationInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirFunctionInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirPropertyInfo
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import com.rsicarelli.fakt.compiler.fir.rendering.formatTypeParameter
import com.rsicarelli.fakt.compiler.fir.rendering.toDeclarationAnnotation
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Metro-inspired compatibility wrapper for accessing extension receiver parameter.
 *
 * Uses the unified `parameters` list instead of deprecated `extensionReceiverParameter`. This
 * approach is future-proof against Kotlin compiler API changes.
 *
 * @return Extension receiver parameter, or null if function is not an extension
 */
private val IrFunction.extensionReceiverParameterCompat: IrValueParameter?
    get() = parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }

/**
 * Transforms FIR metadata (string-based types) to IR generation metadata (IrTypes + IR nodes).
 *
 * This is the critical component that eliminates redundant analysis.
 *
 * ## Transformation Flow
 *
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
 * 1. **Resolves Properties**: FirPropertyInfo (string) → IrPropertyMetadata (IrType + IrProperty
 *    node)
 * 2. **Resolves Functions**: FirFunctionInfo (string) → IrFunctionMetadata (IrType +
 *    IrSimpleFunction node)
 * 3. **Formats Type Parameters**: `["T", "K : Comparable<K>"]`
 * 4. **Computes GenericPattern**: via [GenericPatternAnalyzer]
 * 5. **Pre-computes semantic flags and [RenderedType] side-channels** when a [TypeResolution] is
 *    supplied — populates `isTypeParameter`, `requiresCollectionErasure`, and the rendered short
 *    name + FQN set on every property, parameter, and return type.
 *
 * @property typeResolution Optional type-resolution facade; when provided the transformer
 *   pre-computes semantic flags and [RenderedType] side-channels on all IR metadata records.
 */
internal class FirToIrTransformer(private val typeResolution: TypeResolution? = null) {
    private val patternAnalyzer = GenericPatternAnalyzer()

    /**
     * Transform ValidatedFakeInterface to IrGenerationMetadata.
     *
     * **Critical**: This method uses FIR metadata as the source of truth for structure. IrClass is
     * only used for:
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
    fun transform(firMetadata: ValidatedFakeInterface, irClass: IrClass): IrGenerationMetadata {
        // 1. Resolve directly declared properties (FIR strings → IrTypes + IR nodes)
        val declaredProperties =
            firMetadata.properties.map { firProperty -> resolveProperty(firProperty, irClass) }

        // 2. Resolve directly declared functions (FIR strings → IrTypes + IR nodes)
        val declaredFunctions =
            firMetadata.functions.map { firFunction ->
                resolveFunction(firFunction = firFunction, irClass = irClass)
            }

        // Resolve inherited members
        // Inherited members need to be resolved against the IR class as well
        val inheritedProperties =
            firMetadata.inheritedProperties.map { firProperty ->
                resolveProperty(firProperty, irClass)
            }

        val inheritedFunctions =
            firMetadata.inheritedFunctions.map { firFunction ->
                resolveFunction(firFunction = firFunction, irClass = irClass)
            }

        // Combine declared and inherited members for code generation
        // The fake implementation needs to provide implementations for ALL members
        val allProperties = declaredProperties + inheritedProperties
        val allFunctions = declaredFunctions + inheritedFunctions

        // 3. Format type parameters with bounds
        val typeParameters = firMetadata.typeParameters.map { formatTypeParameter(it) }

        // 4. Transform annotations for propagation
        val annotations = transformAnnotations(firMetadata.annotations)

        // 5. Pass pattern analyzer for lazy computation
        return IrGenerationMetadata(
            interfaceName = firMetadata.simpleName,
            packageName = firMetadata.packageName,
            qualifiedSourceName = firMetadata.qualifiedSourceName,
            typeParameters = typeParameters,
            members =
                IrInterfaceMembers(
                    properties = allProperties,
                    functions = allFunctions,
                    annotations = annotations,
                ),
            sourceInterface = irClass,
            patternAnalyzer = patternAnalyzer,
            config =
                IrGenerationConfig(
                    isFromCache = firMetadata.isFromCache,
                    sourceSourceSet = firMetadata.sourceSourceSet,
                    visibility = firMetadata.visibility,
                    callHistoryMode = firMetadata.callHistoryMode,
                    mutabilityMode = firMetadata.mutabilityMode,
                ),
        )
    }

    /**
     * Transform ValidatedFakeClass to IrClassGenerationMetadata.
     *
     * Similar to transform() but handles abstract classes with separate abstract/open members.
     *
     * Added to support abstract class fake generation.
     *
     * Key differences from interface transformation:
     * - Separates abstract properties/methods (must implement)
     * - Separates open properties/methods (can override)
     * - Both lists need fake implementations (treats them the same)
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
            firMetadata.openProperties.map { firProperty -> resolveProperty(firProperty, irClass) }

        // 3. Resolve abstract methods
        val abstractMethods =
            firMetadata.abstractMethods.map { firFunction -> resolveFunction(firFunction, irClass) }

        // 4. Resolve open methods
        val openMethods =
            firMetadata.openMethods.map { firFunction -> resolveFunction(firFunction, irClass) }

        // 5. Format type parameters with bounds
        val typeParameters = firMetadata.typeParameters.map { formatTypeParameter(it) }

        // 6. Transform annotations for propagation
        val annotations = transformAnnotations(firMetadata.annotations)

        // 7. Pass pattern analyzer for lazy computation
        return IrClassGenerationMetadata(
            className = firMetadata.simpleName,
            packageName = firMetadata.packageName,
            qualifiedSourceName = firMetadata.qualifiedSourceName,
            typeParameters = typeParameters,
            members =
                IrClassMembers(
                    abstractProperties = abstractProperties,
                    openProperties = openProperties,
                    abstractMethods = abstractMethods,
                    openMethods = openMethods,
                    annotations = annotations,
                ),
            sourceClass = irClass,
            patternAnalyzer = patternAnalyzer,
            config =
                IrGenerationConfig(
                    isFromCache = firMetadata.isFromCache,
                    sourceSourceSet = firMetadata.sourceSourceSet,
                    visibility = firMetadata.visibility,
                    callHistoryMode = firMetadata.callHistoryMode,
                    mutabilityMode = firMetadata.mutabilityMode,
                ),
        )
    }

    /**
     * Resolve FirPropertyInfo to IrPropertyMetadata by looking up the IR node and extracting
     * IrType.
     *
     * This method performs a PURE LOOKUP operation - NO analysis or validation:
     * 1. Find IrProperty by name in IrClass.declarations (FIR guarantees it exists)
     * 2. Extract IrType from getter.returnType or backingField.type
     * 3. Preserve mutability/nullability from FIR metadata
     *
     * **Performance**:
     * - Property lookup: O(n) where n = properties in class (typically < 20)
     * - IrType extraction: O(1)
     * - Typical cost: < 100μs per property
     *
     * **Error Handling**: Throws IllegalStateException if:
     * - IrProperty not found by name (indicates FIR/IR phase mismatch - compiler bug)
     * - IrProperty has no type (invalid IR state - compiler bug)
     *
     * These errors should never occur in production as FIR validation guarantees the property
     * exists and is well-formed.
     *
     * @param firProperty FIR property metadata (source of truth for structure)
     * @param irClass IR class for node lookup ONLY
     * @return IrPropertyMetadata with resolved IrType and IR node reference
     * @throws IllegalStateException if IR node lookup fails (compiler bug)
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun resolveProperty(
        firProperty: FirPropertyInfo,
        irClass: IrClass,
    ): IrPropertyMetadata {
        // Lookup IrProperty by name (FIR guarantees this exists)
        val irProperty =
            irClass.declarations.filterIsInstance<IrProperty>().firstOrNull {
                it.name.asString() == firProperty.name
            }
                ?: error(
                    "IrProperty '${firProperty.name}' not found in ${irClass.name}. " +
                        "FIR validation should have ensured this exists."
                )

        // Resolve IrType from IR node
        val resolvedType =
            irProperty.getter?.returnType
                ?: irProperty.backingField?.type
                ?: error(
                    "IrProperty '${firProperty.name}' has no type. " +
                        "This should not happen for valid properties."
                )

        // Pre-compute semantic flags and RenderedType side-channel when resolver is available
        val rendered = typeResolution?.irTypeToRendered(resolvedType, preserveTypeParameters = true)
        val isTypeParm = typeResolution?.isTypeParameter(resolvedType) ?: false
        val needsErasure = typeResolution?.requiresCollectionErasure(resolvedType) ?: false

        return IrPropertyMetadata(
            name = firProperty.name,
            type = resolvedType,
            isMutable = firProperty.isMutable,
            isNullable = firProperty.isNullable,
            irProperty = irProperty,
            isTypeParameter = isTypeParm,
            requiresCollectionErasure = needsErasure,
            renderedType = rendered,
        )
    }

    /**
     * Resolve FirFunctionInfo to IrFunctionMetadata by looking up the IR node and extracting
     * IrTypes.
     *
     * This method performs a PURE LOOKUP operation - NO analysis or validation:
     * 1. Find IrSimpleFunction by name in IrClass.declarations (FIR guarantees it exists)
     * 2. Extract IrType for return type
     * 3. Resolve parameters by matching positions (FIR guarantees same order)
     * 4. Preserve suspend/inline flags from FIR metadata
     * 5. Format method-level type parameters with bounds
     *
     * **Performance**:
     * - Function lookup: O(n) where n = functions in class (typically < 30)
     * - Parameter resolution: O(m) where m = parameters (typically < 5)
     * - IrType extraction: O(1) per parameter
     * - Typical cost: < 200μs per function
     *
     * **Error Handling**: Throws IllegalStateException if:
     * - IrSimpleFunction not found by name (FIR/IR mismatch - compiler bug)
     * - Parameter count mismatch (FIR/IR mismatch - compiler bug)
     *
     * These errors should never occur in production as FIR validation guarantees the function
     * exists with matching parameters.
     *
     * **Parameter Matching**: Parameters are matched by position (index), not by name:
     * - FIR: `fun foo(a: String, b: Int)`
     * - IR: `fun foo(param0: String, param1: Int)`
     * - Match: position 0 → a:String, position 1 → b:Int
     *
     * This works because FIR and IR maintain the same parameter order.
     *
     * @param firFunction FIR function metadata (source of truth for structure)
     * @param irClass IR class for node lookup ONLY
     * @return IrFunctionMetadata with resolved IrTypes and IR node reference
     * @throws IllegalStateException if IR node lookup fails or parameter count mismatches (compiler
     *   bug)
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun resolveFunction(
        firFunction: FirFunctionInfo,
        irClass: IrClass,
    ): IrFunctionMetadata {
        // Lookup IrSimpleFunction by name (FIR guarantees this exists)
        val irFunction =
            irClass.declarations.filterIsInstance<IrSimpleFunction>().firstOrNull {
                it.name.asString() == firFunction.name
            }
                ?: error(
                    "IrSimpleFunction '${firFunction.name}' not found in ${irClass.name}. " +
                        "FIR validation should have ensured this exists."
                )

        // Resolve parameters (match by position - FIR guarantees same order)
        val irRegularParams = irFunction.parameters.filter { it.kind == IrParameterKind.Regular }

        if (irRegularParams.size != firFunction.parameters.size) {
            error(
                "Parameter count mismatch for '${firFunction.name}'. " +
                    "FIR: ${firFunction.parameters.size}, IR: ${irRegularParams.size}"
            )
        }

        val parameters =
            firFunction.parameters.zip(irRegularParams).map { (firParam, irParam) ->
                // Pre-compute semantic flags and RenderedType side-channel per param
                val paramRendered =
                    typeResolution?.irTypeToRendered(irParam.type, preserveTypeParameters = true)
                val paramIsTypeParm = typeResolution?.isTypeParameter(irParam.type) ?: false
                val paramNeedsErasure =
                    typeResolution?.requiresCollectionErasure(irParam.type) ?: false

                IrParameterMetadata(
                    name = firParam.name,
                    type = irParam.type,
                    hasDefaultValue = firParam.hasDefaultValue,
                    defaultValueCode = firParam.defaultValueCode, // Pass through default value code
                    isVararg = firParam.isVararg,
                    isTypeParameter = paramIsTypeParm,
                    requiresCollectionErasure = paramNeedsErasure,
                    renderedType = paramRendered,
                )
            }

        // Format method-level type parameters
        val methodTypeParameters = firFunction.typeParameters.map { formatTypeParameter(it) }

        // Pre-compute RenderedType for the return type
        val returnTypeRendered =
            typeResolution?.irTypeToRendered(irFunction.returnType, preserveTypeParameters = true)

        return IrFunctionMetadata(
            name = firFunction.name,
            parameters = parameters,
            returnType = irFunction.returnType,
            isSuspend = firFunction.isSuspend,
            isInline = firFunction.isInline,
            typeParameters = methodTypeParameters,
            typeParameterBounds = firFunction.typeParameterBounds,
            isOperator = irFunction.isOperator,
            extensionReceiverType = irFunction.extensionReceiverParameterCompat?.type,
            irFunction = irFunction,
            renderedReturnType = returnTypeRendered,
        )
    }

    // ========================================================================
    // Annotation Transformation
    // ========================================================================

    /**
     * Transform FIR annotations to IR-ready annotation metadata.
     *
     * Argument rendering is delegated to the shared FIR annotation renderer
     * ([toDeclarationAnnotation]) so the FIR- and IR-phase emitters cannot drift.
     */
    private fun transformAnnotations(
        annotations: List<FirAnnotationInfo>
    ): List<IrAnnotationMetadata> =
        annotations.map { annotation ->
            val rendered = annotation.toDeclarationAnnotation()
            IrAnnotationMetadata(
                simpleName = rendered.simpleName,
                fullyQualifiedName = rendered.fullyQualifiedName,
                renderedArguments = rendered.renderedArguments,
                isOptInMarker = rendered.isOptInMarker,
            )
        }
}
