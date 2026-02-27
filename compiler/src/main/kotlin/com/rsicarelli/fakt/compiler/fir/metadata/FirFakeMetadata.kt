// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.metadata

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId

/**
 * Call history generation mode extracted from @Fake annotation.
 *
 * This enum mirrors [com.rsicarelli.fakt.CallHistoryMode] from the annotations module, used
 * internally by the compiler to avoid runtime dependency on annotations.
 *
 * Resolution logic (in IR phase):
 * - [DEFAULT]: Use plugin-level `enableCallHistoryDefault` setting
 * - [ENABLED]: Always generate call history (overrides plugin default)
 * - [DISABLED]: Never generate call history (overrides plugin default)
 *
 * @see com.rsicarelli.fakt.CallHistoryMode
 */
enum class FirCallHistoryMode {
    /** Use the plugin-level default from `fakt { enableCallHistory.set(...) }`. */
    DEFAULT,

    /** Always generate call history, regardless of plugin default. */
    ENABLED,

    /** Never generate call history, regardless of plugin default. */
    DISABLED,
}

/**
 * Mutability mode extracted from @Fake annotation.
 *
 * This enum mirrors [com.rsicarelli.fakt.MutabilityMode] from the annotations module, used
 * internally by the compiler to avoid runtime dependency on annotations.
 *
 * Resolution logic (in IR phase):
 * - [DEFAULT]: Use plugin-level `enableMutableFakesDefault` setting
 * - [MUTABLE]: Always generate mutable fake (overrides plugin default)
 * - [IMMUTABLE]: Always generate immutable fake (overrides plugin default)
 *
 * @see com.rsicarelli.fakt.MutabilityMode
 */
enum class FirMutabilityMode {
    /** Use the plugin-level default from `fakt { enableMutableFakes.set(...) }`. */
    DEFAULT,

    /** Always generate immutable fake, regardless of plugin default. */
    IMMUTABLE,

    /** Always generate mutable fake, regardless of plugin default. */
    MUTABLE,
}

/**
 * Metadata for a validated @Fake interface, analyzed in FIR phase and passed to IR generation.
 *
 * This data class represents the complete structural analysis of an interface that:
 * 1. Has been validated as eligible for fake generation (not sealed, not external, etc.)
 * 2. Has had its type parameters and members analyzed
 * 3. Is ready for IR phase to reconstruct GenericPattern and generate code
 *
 * Following Metro pattern: FIR phase analyzes and validates, IR phase generates.
 *
 * Removed `genericPattern` field - it will be reconstructed in IR phase from `typeParameters` and
 * `functions` using IrTypeParameter (which we don't have in FIR).
 *
 * Added `inheritedProperties` and `inheritedFunctions` to support interface inheritance. These are
 * collected from super-interfaces recursively.
 *
 * **Immutable**: Once created in FIR phase, this metadata is never modified. **Thread-safe**:
 * Stored in [FirMetadataStorage] which is thread-safe.
 *
 * @property classId Fully qualified class identifier (e.g., com.example.UserService)
 * @property simpleName Simple class name (e.g., UserService)
 * @property packageName Package name (e.g., com.example)
 * @property typeParameters Class-level type parameters with bounds (e.g.,
 *   ["T", "K : Comparable<K>"])
 * @property properties Properties declared directly in this interface
 * @property functions Functions declared directly in this interface (with method-level type
 *   parameters)
 * @property inheritedProperties Properties inherited from super-interfaces
 * @property inheritedFunctions Functions inherited from super-interfaces
 * @property sourceLocation Location in source code for error reporting
 * @property validationTimeNanos Time spent validating this interface in FIR phase (nanoseconds)
 * @property visibility Visibility of the interface (PUBLIC, INTERNAL, etc.) for explicitApi()
 *   support
 * @property callHistoryMode Call history generation mode from @Fake annotation
 * @property mutabilityMode Mutability mode from @Fake annotation
 */
data class ValidatedFakeInterface(
    val classId: ClassId,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<FirTypeParameterInfo>,
    val properties: List<FirPropertyInfo>,
    val functions: List<FirFunctionInfo>,
    val inheritedProperties: List<FirPropertyInfo>,
    val inheritedFunctions: List<FirFunctionInfo>,
    val annotations: List<FirAnnotationInfo> = emptyList(),
    val sourceLocation: FirSourceLocation,
    val validationTimeNanos: Long,
    val isFromCache: Boolean = false,
    val sourceSourceSet: String? = null,
    val visibility: FirVisibility = FirVisibility.PUBLIC,
    val callHistoryMode: FirCallHistoryMode = FirCallHistoryMode.DEFAULT,
    val mutabilityMode: FirMutabilityMode = FirMutabilityMode.DEFAULT,
)

/**
 * Metadata for a validated @Fake class, analyzed in FIR phase.
 *
 * Similar to [ValidatedFakeInterface] but for abstract/open classes that can be faked.
 *
 * @property classId Fully qualified class identifier
 * @property simpleName Simple class name
 * @property packageName Package name
 * @property typeParameters Class-level type parameters with bounds
 * @property abstractProperties Abstract properties requiring implementation
 * @property openProperties Open properties that can be overridden
 * @property abstractMethods Abstract methods requiring implementation
 * @property openMethods Open methods that can be overridden
 * @property sourceLocation Location in source code
 * @property validationTimeNanos Time spent validating this class in FIR phase (nanoseconds)
 * @property visibility Visibility of the class (PUBLIC, INTERNAL, etc.) for explicitApi() support
 * @property callHistoryMode Call history generation mode from @Fake annotation
 * @property mutabilityMode Mutability mode from @Fake annotation
 */
data class ValidatedFakeClass(
    val classId: ClassId,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<FirTypeParameterInfo>,
    val abstractProperties: List<FirPropertyInfo>,
    val openProperties: List<FirPropertyInfo>,
    val abstractMethods: List<FirFunctionInfo>,
    val openMethods: List<FirFunctionInfo>,
    val annotations: List<FirAnnotationInfo> = emptyList(),
    val sourceLocation: FirSourceLocation,
    val validationTimeNanos: Long,
    val isFromCache: Boolean = false,
    val sourceSourceSet: String? = null,
    val visibility: FirVisibility = FirVisibility.PUBLIC,
    val callHistoryMode: FirCallHistoryMode = FirCallHistoryMode.DEFAULT,
    val mutabilityMode: FirMutabilityMode = FirMutabilityMode.DEFAULT,
)

/**
 * Type parameter information extracted from FIR.
 *
 * Examples:
 * - Simple: `T` → FirTypeParameterInfo("T", emptyList())
 * - Bounded: `T : Comparable<T>` → FirTypeParameterInfo("T", listOf("Comparable<T>"))
 * - Multiple bounds: `T : Comparable<T>, Serializable` → FirTypeParameterInfo("T",
 *   listOf("Comparable<T>", "Serializable"))
 *
 * Note: The multiple bounds example shows how constraints are represented as a list of strings.
 *
 * @property name Type parameter name (e.g., "T", "K", "V")
 * @property bounds Upper bounds/constraints (empty for unbounded, e.g., `T : Any` is implicit)
 */
data class FirTypeParameterInfo(
    val name: String,
    val bounds: List<String>, // String representation for simplicity (e.g., "Comparable<T>")
)

/**
 * Property information extracted from FIR interface/class.
 *
 * @property name Property name
 * @property type Type as string (e.g., "String", "List<T>", "Map<K, V>")
 * @property isMutable True if `var`, false if `val`
 * @property isNullable True if type is nullable (e.g., `String?`)
 */
data class FirPropertyInfo(
    val name: String,
    val type: String, // String representation - will be resolved to IR types later
    val isMutable: Boolean,
    val isNullable: Boolean,
)

/**
 * Function information extracted from FIR interface/class.
 *
 * @property name Function name
 * @property parameters Function parameters
 * @property returnType Return type as string
 * @property isSuspend True if `suspend fun`
 * @property isInline True if `inline fun`
 * @property typeParameters Method-level type parameters (e.g., `<T>`, `<R : TValue>`)
 * @property typeParameterBounds Map of type parameter to its bound (e.g., "R" → "TValue")
 */
data class FirFunctionInfo(
    val name: String,
    val parameters: List<FirParameterInfo>,
    val returnType: String,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<FirTypeParameterInfo>,
    val typeParameterBounds: Map<String, String>, // e.g., "R" → "TValue"
)

/**
 * Function parameter information.
 *
 * Added defaultValueCode for rendering actual default value expressions.
 *
 * @property name Parameter name
 * @property type Parameter type as string
 * @property hasDefaultValue True if parameter has default value
 * @property defaultValueCode Rendered default value expression (e.g., "null", "\"GET\"", "30000L",
 *   "true"). null if no default or if rendering complex expressions failed.
 * @property isVararg True if parameter is vararg
 */
data class FirParameterInfo(
    val name: String,
    val type: String,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?,
    val isVararg: Boolean,
)

/**
 * Source location information for error reporting.
 *
 * Captured in FIR phase where source location information is most accurate. Used if we need to
 * report errors during IR generation.
 *
 * @property filePath Source file path
 * @property startLine Start line number (1-indexed)
 * @property startColumn Start column number (0-indexed)
 * @property endLine End line number (1-indexed)
 * @property endColumn End column number (0-indexed)
 */
data class FirSourceLocation(
    val filePath: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
) {
    companion object {
        /** Unknown location - used as fallback when source info not available. */
        val UNKNOWN =
            FirSourceLocation(
                filePath = "<unknown>",
                startLine = 0,
                startColumn = 0,
                endLine = 0,
                endColumn = 0,
            )

        // Regex to extract source set name from path like "src/commonMain/kotlin/..."
        private val SOURCE_SET_REGEX = Regex("""/src/([^/]+)/kotlin/""")
    }

    /**
     * Human-readable location string for error messages.
     *
     * Example: "UserService.kt:42:15"
     */
    fun toDisplayString(): String = "$filePath:$startLine:$startColumn"

    /**
     * Extracts the source set name from the file path.
     *
     * KMP projects follow the convention: `src/{sourceSetName}/kotlin/...`
     *
     * Examples:
     * - `/path/to/src/commonMain/kotlin/pkg/Service.kt` → "commonMain"
     * - `/path/to/src/iosMain/kotlin/pkg/Service.kt` → "iosMain"
     * - `/path/to/src/jvmMain/kotlin/pkg/Service.kt` → "jvmMain"
     * - `<unknown>` → null
     *
     * @return Source set name, or null if cannot be determined
     */
    fun extractSourceSetName(): String? {
        if (filePath == "<unknown>") return null
        return SOURCE_SET_REGEX.find(filePath)?.groupValues?.get(1)
    }
}

/**
 * Visibility level for generated fakes.
 *
 * Extracted from source interface/class visibility to ensure generated fakes have the same
 * visibility, which is required for `explicitApi()` mode.
 *
 * Example:
 * - `public interface UserService` → FirVisibility.PUBLIC → `public class FakeUserServiceImpl`
 * - `internal interface InternalService` → FirVisibility.INTERNAL → `internal class
 *   FakeInternalServiceImpl`
 */
enum class FirVisibility {
    PUBLIC,
    INTERNAL,
    PRIVATE,
    PROTECTED;

    companion object {
        /**
         * Converts Kotlin compiler Visibility to FirVisibility.
         *
         * Maps visibility descriptors to our simplified enum:
         * - DescriptorVisibilities.PUBLIC → PUBLIC
         * - DescriptorVisibilities.INTERNAL → INTERNAL
         * - DescriptorVisibilities.PRIVATE → PRIVATE
         * - DescriptorVisibilities.PROTECTED → PROTECTED
         * - Other (LOCAL, INHERITED, etc.) → PUBLIC (safe default)
         *
         * @param visibility Kotlin compiler visibility descriptor
         * @return Corresponding FirVisibility enum value
         */
        fun from(visibility: Visibility): FirVisibility =
            when (visibility) {
                Visibilities.Public -> PUBLIC
                Visibilities.Internal -> INTERNAL
                Visibilities.Private -> PRIVATE
                Visibilities.Protected -> PROTECTED
                else -> PUBLIC
            }
    }
}

/**
 * Converts FirVisibility to a Kotlin visibility modifier string for code generation.
 *
 * Used by code generators to prepend the correct visibility modifier to generated classes,
 * functions, and properties for `explicitApi()` compatibility.
 *
 * Private and protected are not supported for top-level declarations, so they fallback to public.
 *
 * @return Visibility modifier string with trailing space (e.g., "public ", "internal ")
 */
fun FirVisibility.toModifier(): String =
    when (this) {
        FirVisibility.PUBLIC,
        FirVisibility.PRIVATE,
        FirVisibility.PROTECTED -> "public "

        FirVisibility.INTERNAL -> "internal "
    }

/**
 * Annotation argument value extracted from FIR.
 *
 * Represents the different types of values that can be passed as annotation arguments. Supports
 * class references, literals, enum values, arrays, and nested annotations.
 *
 * Examples:
 * - `@OptIn(ExperimentalApi::class)` → ClassReference("com.example.ExperimentalApi")
 * - `@Deprecated("use foo")` → StringLiteral("use foo")
 * - `@Target(AnnotationTarget.CLASS)` → EnumValue("kotlin.annotation.AnnotationTarget", "CLASS")
 * - `@JvmName(name = "foo")` → StringLiteral("foo") with key "name"
 */
sealed interface FirAnnotationArgument {
    /**
     * Class reference argument (e.g., `ExperimentalApi::class`).
     *
     * @property classId Fully qualified class ID (e.g., "com.example.ExperimentalApi")
     */
    data class ClassReference(val classId: String) : FirAnnotationArgument

    /**
     * String literal argument (e.g., `"deprecated message"`).
     *
     * @property value The string value
     */
    data class StringLiteral(val value: String) : FirAnnotationArgument

    /**
     * Number literal argument (e.g., `42`, `3.14`).
     *
     * @property value String representation of the number
     */
    data class NumberLiteral(val value: String) : FirAnnotationArgument

    /**
     * Boolean literal argument (e.g., `true`, `false`).
     *
     * @property value The boolean value
     */
    data class BooleanLiteral(val value: Boolean) : FirAnnotationArgument

    /**
     * Char literal argument (e.g., `'a'`).
     *
     * @property value The char value
     */
    data class CharLiteral(val value: Char) : FirAnnotationArgument

    /**
     * Enum value argument (e.g., `AnnotationTarget.CLASS`).
     *
     * @property enumClassId Fully qualified enum class ID
     * @property entryName Enum entry name (e.g., "CLASS")
     */
    data class EnumValue(val enumClassId: String, val entryName: String) : FirAnnotationArgument

    /**
     * Array argument (e.g., `[Foo::class, Bar::class]`).
     *
     * @property elements List of array element values
     */
    data class ArrayValue(val elements: List<FirAnnotationArgument>) : FirAnnotationArgument

    /**
     * Nested annotation argument (e.g., `@Nested("value")`).
     *
     * @property annotation The nested annotation info
     */
    data class NestedAnnotation(val annotation: FirAnnotationInfo) : FirAnnotationArgument
}

/**
 * Annotation information extracted from FIR phase.
 *
 * Contains the annotation's class ID and its arguments (if any). Used to propagate annotations from
 * source types to generated fakes.
 *
 * Examples:
 * - `@OptIn(ExperimentalApi::class)` → FirAnnotationInfo( annotationClassId = "kotlin.OptIn",
 *   arguments = mapOf("markerClasses" to
 *   ArrayValue([ClassReference("com.example.ExperimentalApi")])) )
 * - `@Deprecated("old")` → FirAnnotationInfo( annotationClassId = "kotlin.Deprecated", arguments =
 *   mapOf("message" to StringLiteral("old")) )
 *
 * @property annotationClassId Fully qualified annotation class ID (e.g., "kotlin.OptIn")
 * @property arguments Named arguments to the annotation (empty for marker annotations)
 * @property isOptInMarker True if this annotation is marked with @RequiresOptIn. When true, the
 *   generated fake needs @OptIn(ThisAnnotation::class) to compile.
 */
data class FirAnnotationInfo(
    val annotationClassId: String,
    val arguments: Map<String, FirAnnotationArgument> = emptyMap(),
    val isOptInMarker: Boolean = false,
)
