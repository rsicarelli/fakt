// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.analysis

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Pure (compiler-API-free) contract for a `@Fake`-annotated declaration.
 *
 * All type information arrives **pre-rendered** (e.g. `"List<User>?"`) so that codegen-runtime
 * never needs to import `org.jetbrains.kotlin.ir.*`. The IR-side translator in `:compiler` is
 * responsible for resolving every `IrType` to its rendered shape before invoking codegen.
 *
 * The [Interface] and [Class] variants share a common set of fields via the sealed interface and
 * add variant-specific ones below.
 */
sealed interface FakeDeclaration {
    /** Simple name of the source declaration (e.g. `"UserRepository"`). */
    val simpleName: String

    /**
     * Fully-qualified name of the source declaration (e.g. `"com.example.auth.UserRepository"`).
     */
    val qualifiedSourceName: String

    /** Package of the source declaration (e.g. `"com.example.auth"`). */
    val packageName: String

    /** Class-level type parameters with optional bounds (e.g. `["T", "K : Comparable<K>"]`). */
    val typeParameters: List<String>

    /** Visibility of the source declaration for `explicitApi()` support. */
    val visibility: FirVisibility

    /** Annotations declared on the source that should be propagated to the generated fake. */
    val annotations: List<DeclarationAnnotation>

    /**
     * Fully-qualified names that must be imported in the generated file. Rolled up by the IR-side
     * translator from each member type's FQN side-channel; consumed by
     * [ImportResolver][com.rsicarelli.fakt.compiler.core.context.ImportResolver].
     */
    val requiredImports: Set<String>

    /** Whether call-history tracking code should be generated. */
    val generateCallHistory: Boolean

    /** Whether mutable-behavior support should be generated. */
    val generateMutableBehaviors: Boolean

    // -------------------------------------------------------------------------
    // Variants
    // -------------------------------------------------------------------------

    /**
     * A `@Fake`-annotated **interface**.
     *
     * @property genericPattern Classification of how generics are used — pure string form,
     *   constructed by the IR-side translator.
     * @property properties All properties that the fake must implement.
     * @property functions All functions that the fake must implement.
     */
    data class Interface(
        override val simpleName: String,
        override val qualifiedSourceName: String,
        override val packageName: String,
        override val typeParameters: List<String>,
        override val visibility: FirVisibility,
        override val annotations: List<DeclarationAnnotation>,
        override val requiredImports: Set<String>,
        override val generateCallHistory: Boolean,
        override val generateMutableBehaviors: Boolean,
        val genericPattern: PureGenericPattern,
        val properties: List<PropertySpec>,
        val functions: List<FunctionSpec>,
    ) : FakeDeclaration

    /**
     * A `@Fake`-annotated **abstract or open class**.
     *
     * Abstract and open members are kept separate so generators can apply the correct default
     * strategy: `error(...)` for abstract, `super.xxx()` for open.
     *
     * @property constructorParameters Primary-constructor parameters that must be forwarded in the
     *   `super(...)` call.
     * @property abstractMethods Methods that must be overridden.
     * @property openMethods Methods that *may* be overridden (super delegation as default).
     * @property abstractProperties Properties that must be overridden.
     * @property openProperties Properties that *may* be overridden.
     */
    data class Class(
        override val simpleName: String,
        override val qualifiedSourceName: String,
        override val packageName: String,
        override val typeParameters: List<String>,
        override val visibility: FirVisibility,
        override val annotations: List<DeclarationAnnotation>,
        override val requiredImports: Set<String>,
        override val generateCallHistory: Boolean,
        override val generateMutableBehaviors: Boolean,
        val constructorParameters: List<ConstructorParam>,
        val abstractMethods: List<FunctionSpec>,
        val openMethods: List<FunctionSpec>,
        val abstractProperties: List<PropertySpec>,
        val openProperties: List<PropertySpec>,
    ) : FakeDeclaration
}

// -------------------------------------------------------------------------
// Spec records
// -------------------------------------------------------------------------

/**
 * Pre-rendered specification of a property to be faked.
 *
 * @property name Property name.
 * @property typeString Pre-rendered type string (e.g. `"List<User>?"`).
 * @property isMutable `true` for `var`, `false` for `val`.
 * @property isNullable `true` if the type is nullable (`T?`).
 * @property isTypeParameter `true` when [typeString] is a type-parameter placeholder (T, K, V …).
 * @property requiresCollectionErasure `true` when [typeString] is a stdlib collection whose type
 *   arguments must be erased to `Any` under the NoGenerics pattern.
 */
data class PropertySpec(
    val name: String,
    val typeString: String,
    val isMutable: Boolean,
    val isNullable: Boolean,
    val isTypeParameter: Boolean = false,
    val requiresCollectionErasure: Boolean = false,
)

/**
 * Pre-rendered specification of a function to be faked.
 *
 * @property returnTypeString Pre-rendered return type (e.g. `"suspend () -> Unit"`).
 * @property extensionReceiverTypeString Pre-rendered extension receiver type, or `null` for
 *   non-extension functions.
 * @property typeParameters Method-level type parameters with bounds (e.g. `["T", "R :
 *   Comparable<R>"]`).
 * @property typeParameterBounds Method-level type-parameter bounds map (e.g. `"R" → "TValue"`).
 */
data class FunctionSpec(
    val name: String,
    val parameters: List<ParameterSpec>,
    val returnTypeString: String,
    val extensionReceiverTypeString: String?,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val isOperator: Boolean,
    val typeParameters: List<String>,
    val typeParameterBounds: Map<String, String>,
)

/**
 * Pre-rendered specification of a function parameter.
 *
 * @property defaultValueCode Rendered default-value expression, or `null` if the parameter has no
 *   default.
 * @property isTypeParameter `true` when [typeString] is a type-parameter placeholder.
 * @property requiresCollectionErasure `true` when [typeString] is a stdlib collection whose type
 *   arguments must be erased to `Any` under the NoGenerics pattern.
 */
data class ParameterSpec(
    val name: String,
    val typeString: String,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?,
    val isVararg: Boolean,
    val isTypeParameter: Boolean = false,
    val requiresCollectionErasure: Boolean = false,
)

/**
 * Pre-rendered specification of an annotation propagated to the generated fake.
 *
 * @property simpleName Simple annotation name (e.g. `"OptIn"`).
 * @property fullyQualifiedName Fully-qualified name for import resolution (e.g. `"kotlin.OptIn"`).
 * @property renderedArguments Pre-rendered argument strings (e.g. `["ExperimentalApi::class"]`).
 * @property isOptInMarker `true` when this annotation is itself marked `@RequiresOptIn` — the
 *   generated fake then needs `@OptIn(ThisAnnotation::class)` to compile.
 */
data class DeclarationAnnotation(
    val simpleName: String,
    val fullyQualifiedName: String,
    val renderedArguments: List<String> = emptyList(),
    val isOptInMarker: Boolean = false,
)

/**
 * Pre-rendered constructor parameter for abstract-class fakes.
 *
 * @property name Parameter name.
 * @property typeString Rendered type string (e.g. `"String"`, `"Int"`).
 * @property hasDefault `true` if the parameter has a default value in the super class.
 */
data class ConstructorParam(val name: String, val typeString: String, val hasDefault: Boolean)

// -------------------------------------------------------------------------
// Pure generic pattern (no IR dependencies)
// -------------------------------------------------------------------------

/**
 * Compiler-API-free classification of how generics are used in a declaration.
 *
 * This is the pure analogue of the IR-coupled `GenericPattern` sealed class that lives in
 * `:compiler`. The IR-side translator maps the IR variant to this pure one and stores it on
 * [FakeDeclaration.Interface.genericPattern].
 *
 * All type information is expressed as strings so that codegen-runtime never imports
 * `org.jetbrains.kotlin.ir.*`.
 */
sealed interface PureGenericPattern {
    /** No generic parameters at all. */
    object NoGenerics : PureGenericPattern

    /**
     * Generic parameters at class level only (e.g. `interface Repo<T>`).
     *
     * @property typeParameterNames Simple names of the class-level type parameters (e.g. `["T"]`).
     */
    data class ClassLevelGenerics(val typeParameterNames: List<String>) : PureGenericPattern

    /**
     * Generic parameters at method level only.
     *
     * @property methodNames Names of the methods that carry type parameters.
     */
    data class MethodLevelGenerics(val methodNames: List<String>) : PureGenericPattern

    /**
     * Both class-level and method-level generics.
     *
     * @property typeParameterNames Class-level type parameter names.
     * @property methodNames Method names that carry their own type parameters.
     */
    data class MixedGenerics(val typeParameterNames: List<String>, val methodNames: List<String>) :
        PureGenericPattern
}
