// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.transform

// RenderedType import needed for the side-channel accessors added in 3.1.d.1
import com.rsicarelli.fakt.compiler.core.types.RenderedType
import com.rsicarelli.fakt.compiler.fir.metadata.FirCallHistoryMode
import com.rsicarelli.fakt.compiler.fir.metadata.FirMutabilityMode
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Generation configuration for IR metadata.
 *
 * Groups cache, visibility, and call history settings to reduce constructor parameter count. Used
 * by both [IrGenerationMetadata] and [IrClassGenerationMetadata].
 *
 * @property isFromCache Whether metadata was loaded from incremental compilation cache
 * @property sourceSourceSet Source set identifier for generated code placement
 * @property visibility Visibility of the interface/class for explicitApi() support
 * @property callHistoryMode Call history generation mode from @Fake annotation
 * @property mutabilityMode Mutability mode from @Fake annotation
 */
data class IrGenerationConfig(
    val isFromCache: Boolean = false,
    val sourceSourceSet: String? = null,
    val visibility: FirVisibility = FirVisibility.PUBLIC,
    val callHistoryMode: FirCallHistoryMode = FirCallHistoryMode.DEFAULT,
    val mutabilityMode: FirMutabilityMode = FirMutabilityMode.DEFAULT,
)

/**
 * Interface members with resolved IR types.
 *
 * Groups all interface members (properties and functions) to reduce constructor parameter count.
 *
 * @property properties All interface properties with resolved IrTypes
 * @property functions All interface functions with resolved IrTypes
 * @property annotations Annotations from the interface for propagation
 */
data class IrInterfaceMembers(
    val properties: List<IrPropertyMetadata>,
    val functions: List<IrFunctionMetadata>,
    val annotations: List<IrAnnotationMetadata> = emptyList(),
)

/**
 * Abstract class members with resolved IR types.
 *
 * Groups all class members separated by abstract/open to reduce constructor parameter count.
 *
 * @property abstractProperties Abstract properties (must be implemented)
 * @property openProperties Open properties (can be overridden)
 * @property abstractMethods Abstract methods (must be implemented)
 * @property openMethods Open methods (can be overridden)
 * @property annotations Annotations from the class for propagation
 */
data class IrClassMembers(
    val abstractProperties: List<IrPropertyMetadata>,
    val openProperties: List<IrPropertyMetadata>,
    val abstractMethods: List<IrFunctionMetadata>,
    val openMethods: List<IrFunctionMetadata>,
    val annotations: List<IrAnnotationMetadata> = emptyList(),
)

/**
 * Metadata for IR code generation, transformed from FIR ValidatedFakeInterface.
 *
 * This is the **bridge** between FIR phase (string-based types) and IR phase (IrTypes). Eliminates
 * the need for re-analyzing IrClass instances.
 *
 * This API enables proper FIR→IR communication following Metro pattern:
 * - FIR analyzes and extracts metadata (strings)
 * - FirToIrTransformer resolves strings → IrTypes and lookups IR nodes
 * - IR generation uses this metadata WITHOUT re-analysis
 *
 * **Performance Optimization**: `genericPattern` is computed lazily on first access. This avoids
 * expensive pattern analysis during FIR→IR transformation for interfaces that are skipped by
 * caching or fail validation. Analysis only happens when code generation actually needs the pattern
 * information (~40% cache hit rate).
 *
 * @property interfaceName Simple interface name (e.g., "UserRepository")
 * @property packageName Package name (e.g., "com.example")
 * @property typeParameters Class-level type parameters with bounds (e.g.,
 *   ["T", "K : Comparable<K>"])
 * @property members Interface members (properties, functions, annotations)
 * @property genericPattern Classification of generic usage (NoGenerics, ClassLevel, MethodLevel,
 *   Mixed) - computed lazily
 * @property sourceInterface Original IrClass for code generation context
 * @property config Generation configuration (visibility, cache state, call history mode)
 */
class IrGenerationMetadata
internal constructor(
    val interfaceName: String,
    val packageName: String,
    val qualifiedSourceName: String,
    val typeParameters: List<String>,
    private val members: IrInterfaceMembers,
    val sourceInterface: IrClass,
    private val patternAnalyzer: GenericPatternAnalyzer,
    private val config: IrGenerationConfig = IrGenerationConfig(),
) {
    /** All interface properties with resolved IrTypes. */
    val properties: List<IrPropertyMetadata>
        get() = members.properties

    /** All interface functions with resolved IrTypes. */
    val functions: List<IrFunctionMetadata>
        get() = members.functions

    /** Annotations from the interface for propagation. */
    val annotations: List<IrAnnotationMetadata>
        get() = members.annotations

    /** Whether metadata was loaded from incremental compilation cache. */
    val isFromCache: Boolean
        get() = config.isFromCache

    /** Source set identifier for generated code placement. */
    val sourceSourceSet: String?
        get() = config.sourceSourceSet

    /** Visibility of the interface for explicitApi() support. */
    val visibility: FirVisibility
        get() = config.visibility

    /** Call history generation mode from @Fake annotation. */
    val callHistoryMode: FirCallHistoryMode
        get() = config.callHistoryMode

    /** Mutability mode from @Fake annotation. */
    val mutabilityMode: FirMutabilityMode
        get() = config.mutabilityMode

    /**
     * Lazy generic pattern analysis - computed on first access only.
     *
     * Most interfaces have no generics or simple patterns. Deferring this expensive analysis
     * (25-40% of FIR→IR transform time) until actually needed provides significant performance
     * improvement.
     *
     * Thread-safe via Kotlin's lazy delegate (SYNCHRONIZED mode by default).
     */
    val genericPattern: GenericPattern by lazy { patternAnalyzer.analyzeInterface(sourceInterface) }
}

/**
 * Property metadata with resolved IR types.
 *
 * Transformed from FirPropertyInfo (strings) to IR-ready structure.
 *
 * **3.1.d.1 additions**:
 * - [isTypeParameter] — pre-computed semantic flag; true when the property type is a type-parameter
 *   placeholder (T, K, V …). Mirrors the `is IrTypeParameter` check previously buried in
 *   [com.rsicarelli.fakt.compiler.core.types.TypeRenderer.handleComplexType].
 * - [requiresCollectionErasure] — pre-computed semantic flag; true when the property type is a
 *   collection whose type arguments must be erased to `Any` under the NoGenerics pattern. Mirrors
 *   the rules previously inside [com.rsicarelli.fakt.compiler.core.types.GenericTypeHandler].
 * - [renderedType] — [RenderedType] side channel (short name + FQN set). Populated during FIR→IR
 *   transform when a [com.rsicarelli.fakt.compiler.core.types.TypeResolution] is available.
 *
 * @property name Property name
 * @property type Resolved IrType (from FIR string representation)
 * @property isMutable true for `var`, false for `val`
 * @property isNullable true if type is nullable (T?)
 * @property irProperty Original IR property node (for code generation)
 * @property isTypeParameter true when the property type is a type-parameter (3.1.d.1)
 * @property requiresCollectionErasure true when type args must be erased to Any (3.1.d.1)
 * @property renderedType Pre-rendered type with FQN side-channel, or null if not yet computed
 */
data class IrPropertyMetadata(
    val name: String,
    val type: IrType,
    val isMutable: Boolean,
    val isNullable: Boolean,
    val irProperty: IrProperty,
    val isTypeParameter: Boolean = false,
    val requiresCollectionErasure: Boolean = false,
    val renderedType: RenderedType? = null,
)

/**
 * Function metadata with resolved IR types.
 *
 * Transformed from FirFunctionInfo (strings) to IR-ready structure.
 *
 * **3.1.d.1 addition**: [renderedReturnType] — [RenderedType] side channel for the return type
 * (short name + FQN set). Populated during FIR→IR transform when a
 * [com.rsicarelli.fakt.compiler.core.types.TypeResolution] is available.
 *
 * @property name Function name
 * @property parameters Function parameters with resolved IrTypes
 * @property returnType Resolved return IrType (from FIR string representation)
 * @property isSuspend true if function is suspend
 * @property isInline true if function is inline
 * @property typeParameters Method-level type parameters with bounds (e.g.,
 *   ["T", "R : Comparable<R>"])
 * @property typeParameterBounds Method-level type parameter bounds map (e.g., "R" → "TValue")
 * @property irFunction Original IR function node (for code generation)
 * @property renderedReturnType Pre-rendered return type with FQN side-channel, or null (3.1.d.1)
 */
data class IrFunctionMetadata(
    val name: String,
    val parameters: List<IrParameterMetadata>,
    val returnType: IrType,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<String>,
    val typeParameterBounds: Map<String, String>,
    val isOperator: Boolean,
    val extensionReceiverType: IrType?,
    val irFunction: IrSimpleFunction,
    val renderedReturnType: RenderedType? = null,
)

/**
 * Parameter metadata with resolved IR types.
 *
 * Transformed from FirParameterInfo (strings) to IR-ready structure.
 *
 * Added defaultValueCode for default parameter support in generated code.
 *
 * **3.1.d.1 additions**:
 * - [isTypeParameter] — pre-computed semantic flag; true when the parameter type is a
 *   type-parameter placeholder.
 * - [requiresCollectionErasure] — pre-computed semantic flag; true when the parameter type is a
 *   collection that would be erased under the NoGenerics pattern.
 * - [renderedType] — [RenderedType] side channel (short name + FQN set).
 *
 * @property name Parameter name
 * @property type Resolved IrType (from FIR string representation)
 * @property hasDefaultValue true if parameter has default value
 * @property defaultValueCode Rendered default value code (e.g., "null", "\"GET\"", "30000L")
 * @property isVararg true if parameter is vararg
 * @property isTypeParameter true when the parameter type is a type-parameter (3.1.d.1)
 * @property requiresCollectionErasure true when type args must be erased to Any (3.1.d.1)
 * @property renderedType Pre-rendered type with FQN side-channel, or null if not yet computed
 */
data class IrParameterMetadata(
    val name: String,
    val type: IrType,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?,
    val isVararg: Boolean,
    val isTypeParameter: Boolean = false,
    val requiresCollectionErasure: Boolean = false,
    val renderedType: RenderedType? = null,
)

/**
 * Metadata for IR code generation from abstract classes, transformed from FIR ValidatedFakeClass.
 *
 * This is the **bridge** between FIR phase (string-based types) and IR phase (IrTypes) for abstract
 * classes. Similar to IrGenerationMetadata but separates abstract and open members.
 *
 * Added to support abstract class fake generation.
 * - FIR analyzes and extracts metadata (strings) with abstract/open separation
 * - FirToIrTransformer resolves strings → IrTypes and lookups IR nodes
 * - IR generation uses this metadata WITHOUT re-analysis
 *
 * **Performance Optimization**: `genericPattern` is computed lazily on first access. See
 * IrGenerationMetadata for detailed rationale.
 *
 * @property className Simple class name (e.g., "AbstractRepository")
 * @property packageName Package name (e.g., "com.example")
 * @property typeParameters Class-level type parameters with bounds (e.g.,
 *   ["T", "K : Comparable<K>"])
 * @property members Class members separated by abstract/open
 * @property genericPattern Classification of generic usage (NoGenerics, ClassLevel, MethodLevel,
 *   Mixed) - computed lazily
 * @property sourceClass Original IrClass for code generation context
 * @property config Generation configuration (visibility, cache state, call history mode)
 */
class IrClassGenerationMetadata
internal constructor(
    val className: String,
    val packageName: String,
    val qualifiedSourceName: String,
    val typeParameters: List<String>,
    private val members: IrClassMembers,
    val sourceClass: IrClass,
    private val patternAnalyzer: GenericPatternAnalyzer,
    private val config: IrGenerationConfig = IrGenerationConfig(),
) {
    /** Abstract properties (must be implemented). */
    val abstractProperties: List<IrPropertyMetadata>
        get() = members.abstractProperties

    /** Open properties (can be overridden). */
    val openProperties: List<IrPropertyMetadata>
        get() = members.openProperties

    /** Abstract methods (must be implemented). */
    val abstractMethods: List<IrFunctionMetadata>
        get() = members.abstractMethods

    /** Open methods (can be overridden). */
    val openMethods: List<IrFunctionMetadata>
        get() = members.openMethods

    /** Annotations from the class for propagation. */
    val annotations: List<IrAnnotationMetadata>
        get() = members.annotations

    /** Whether metadata was loaded from incremental compilation cache. */
    val isFromCache: Boolean
        get() = config.isFromCache

    /** Source set identifier for generated code placement. */
    val sourceSourceSet: String?
        get() = config.sourceSourceSet

    /** Visibility of the class for explicitApi() support. */
    val visibility: FirVisibility
        get() = config.visibility

    /** Call history generation mode from @Fake annotation. */
    val callHistoryMode: FirCallHistoryMode
        get() = config.callHistoryMode

    /** Mutability mode from @Fake annotation. */
    val mutabilityMode: FirMutabilityMode
        get() = config.mutabilityMode

    /**
     * Lazy generic pattern analysis - computed on first access only. See
     * IrGenerationMetadata.genericPattern for details.
     */
    val genericPattern: GenericPattern by lazy { patternAnalyzer.analyzeInterface(sourceClass) }
}

/**
 * Annotation metadata for IR code generation.
 *
 * Contains pre-rendered annotation information ready for code generation. Arguments are
 * pre-rendered to Kotlin source code strings.
 *
 * Examples:
 * - `@OptIn(ExperimentalApi::class)` → IrAnnotationMetadata( simpleName = "OptIn",
 *   fullyQualifiedName = "kotlin.OptIn", renderedArguments = ["ExperimentalApi::class"] )
 * - `@Deprecated("old", level = DeprecationLevel.WARNING)` → IrAnnotationMetadata( simpleName =
 *   "Deprecated", fullyQualifiedName = "kotlin.Deprecated", renderedArguments =
 *   ["\"old\"", "level = DeprecationLevel.WARNING"] )
 *
 * @property simpleName Simple annotation name (e.g., "OptIn", "Deprecated")
 * @property fullyQualifiedName Fully qualified name for imports (e.g., "kotlin.OptIn")
 * @property renderedArguments Pre-rendered argument strings for code generation
 * @property isOptInMarker True if this annotation is marked with @RequiresOptIn. When true, the
 *   generated fake needs @OptIn(ThisAnnotation::class) to compile.
 */
data class IrAnnotationMetadata(
    val simpleName: String,
    val fullyQualifiedName: String,
    val renderedArguments: List<String> = emptyList(),
    val isOptInMarker: Boolean = false,
)
