// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.types

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isByte
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Handles type resolution and conversion from IR types to Kotlin string representations.
 *
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class TypeResolver {
    /**
     * Converts IR type to readable Kotlin string representation.
     *
     * @param irType The IR type to convert
     * @return String representation of the type
     */
    fun irTypeToKotlinString(irType: IrType): String = irTypeToKotlinString(irType, preserveTypeParameters = false)

    /**
     * Converts IR type to readable Kotlin string representation with type substitution.
     *
     * This method applies type parameter substitution (e.g., T -> String) before converting
     * to string representation. Essential for generic fake generation.
     *
     * Example:
     * ```
     * interface Repository<T> { fun save(item: T): T }
     *
     * // With substitutor mapping T -> String:
     * irTypeToKotlinStringWithSubstitution(TType, substitutor) // "String"
     * ```
     *
     * @param irType The IR type to convert
     * @param substitutor The type substitutor to apply
     * @return String representation with substitutions applied
     */
    fun irTypeToKotlinStringWithSubstitution(
        irType: IrType,
        substitutor: IrTypeSubstitutor,
    ): String {
        // First apply the substitution, then convert to string
        val substitutedType = substitutor.substitute(irType)

        // Convert the substituted type to string, preserving any remaining type parameters
        return irTypeToKotlinString(substitutedType, preserveTypeParameters = true)
    }

    /**
     * Converts IR type to readable Kotlin string representation with optional type parameter preservation.
     *
     * @param irType The IR type to convert
     * @param preserveTypeParameters Whether to preserve generic type parameters
     * @return String representation of the type
     */
    fun irTypeToKotlinString(
        irType: IrType,
        preserveTypeParameters: Boolean,
    ): String =
        when {
            // Handle nullable types
            irType.isMarkedNullable() -> {
                val baseType = irTypeToKotlinString(irType.makeNotNull(), preserveTypeParameters)
                val nonNullType = irType.makeNotNull()

                // Function types need parentheses when nullable: ((Int, Int) -> Unit)?
                if (isFunction(nonNullType) || isSuspendFunction(nonNullType)) {
                    "($baseType)?"
                } else {
                    "$baseType?"
                }
            }

            // Handle primitive types
            else ->
                irType.asPrimitiveName()
                    ?: handleComplexType(irType, preserveTypeParameters)
        }

    /**
     * Returns primitive type name or null if not primitive.
     * Extracted to reduce complexity of irTypeToKotlinString().
     */
    private fun IrType.asPrimitiveName(): String? =
        when {
            isString() -> "String"
            isInt() -> "Int"
            isBoolean() -> "Boolean"
            isUnit() -> "Unit"
            isLong() -> "Long"
            isFloat() -> "Float"
            isDouble() -> "Double"
            isChar() -> "Char"
            isByte() -> "Byte"
            isShort() -> "Short"
            isNothing() -> "Nothing"
            isAny() -> "Any"
            else -> null
        }

    /**
     * Handles complex (non-primitive) type conversion.
     * Extracted to reduce complexity of irTypeToKotlinString().
     */
    private fun handleComplexType(
        irType: IrType,
        preserveTypeParameters: Boolean,
    ): String =
        when {
            // Handle function types - check by class name pattern
            isFunction(irType) -> handleFunctionType(irType, preserveTypeParameters)

            // Handle suspending function types
            isSuspendFunction(irType) -> {
                val baseFunctionType = handleFunctionType(irType, preserveTypeParameters)
                "suspend $baseFunctionType"
            }

            // Handle type parameters (T, K, V, etc.)
            irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> {
                val typeParam = irType.classifier.owner as IrTypeParameter
                if (preserveTypeParameters) {
                    typeParam.name.asString()
                } else {
                    // For NoGenerics pattern, use Any for type erasure
                    "Any"
                }
            }

            // Handle generic types
            irType is IrSimpleType && irType.arguments.isNotEmpty() -> {
                handleGenericType(irType, preserveTypeParameters)
            }

            // Handle regular class types
            else -> {
                val irClass = irType.getClass()
                if (irClass != null) {
                    getSimpleClassName(irClass)
                } else {
                    // Fallback for unresolved types
                    irType.toString().substringAfterLast('.')
                }
            }
        }

    /**
     * Generates appropriate default values for IR types.
     *
     * @param irType The type to generate a default value for
     * @return String representation of the default value
     */
    fun getDefaultValue(irType: IrType): String =
        getPrimitiveDefault(irType)
            ?: when {
                irType.isMarkedNullable() -> "null"
                irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> "Any()"
                isFunction(irType) || isSuspendFunction(irType) -> generateFunctionDefault(irType)
                else -> handleClassDefault(irType)
            }

    /**
     * Returns default value for primitive types, or null if not a primitive.
     * Extracted to reduce complexity of getDefaultValue().
     *
     * @param irType The type to check
     * @return Default value string for primitives, or null for non-primitives
     */
    private fun getPrimitiveDefault(irType: IrType): String? =
        when {
            irType.isString() -> "\"\""
            irType.isInt() -> "0"
            irType.isBoolean() -> "false"
            irType.isUnit() -> "Unit"
            irType.isLong() -> "0L"
            irType.isFloat() -> "0.0f"
            irType.isDouble() -> "0.0"
            irType.isChar() -> "'\\u0000'"
            irType.isByte() -> "0"
            irType.isShort() -> "0"
            irType.isNothing() -> "TODO(\"Nothing type has no values\")"
            irType.isAny() -> "Any()"
            else -> null
        }

    /**
     * Handles function type conversion to Kotlin syntax.
     */
    private fun handleFunctionType(
        irType: IrType,
        preserveTypeParameters: Boolean,
    ): String {
        // Extract function arity from class name (Function0, Function1, etc.)
        val irClass = irType.getClass()
        val className = irClass?.name?.asString() ?: ""

        if (className.startsWith("Function") || className.startsWith("SuspendFunction")) {
            val arityString =
                when {
                    className.startsWith("SuspendFunction") -> className.removePrefix("SuspendFunction")
                    className.startsWith("Function") -> className.removePrefix("Function")
                    else -> ""
                }
            val arity = arityString.toIntOrNull() ?: 0

            if (irType is IrSimpleType && irType.arguments.size == arity + 1) {
                val paramTypes =
                    irType.arguments.take(arity).map { arg ->
                        if (arg is IrTypeProjection) {
                            irTypeToKotlinString(arg.type, preserveTypeParameters)
                        } else {
                            "Any"
                        }
                    }
                val returnType =
                    irType.arguments.lastOrNull()?.let { arg ->
                        if (arg is IrTypeProjection) {
                            irTypeToKotlinString(arg.type, preserveTypeParameters)
                        } else {
                            "Any"
                        }
                    } ?: "Unit"

                return if (paramTypes.isEmpty()) {
                    "() -> $returnType"
                } else {
                    "(${paramTypes.joinToString(", ")}) -> $returnType"
                }
            }
        }

        // Fallback
        return "() -> Unit"
    }

    /**
     * Handles generic type conversion with proper type parameter handling.
     */
    private fun handleGenericType(
        irType: IrSimpleType,
        preserveTypeParameters: Boolean,
    ): String {
        val irClass = irType.getClass() ?: return "Any"
        val className = getSimpleClassName(irClass)
        val packageName = irClass.kotlinFqName.parent().asString()

        return when {
            preserveTypeParameters && irType.arguments.isNotEmpty() -> {
                val typeArgsString = typeArgumentsToString(irType.arguments, preserveTypeParameters)
                "$className$typeArgsString"
            }
            // NoGenerics pattern: Use specific type erasure rules for common types
            packageName == "kotlin.collections" && className in
                listOf(
                    "List",
                    "MutableList",
                )
            -> "List<Any>"

            packageName == "kotlin.collections" && className in
                listOf(
                    "Set",
                    "MutableSet",
                )
            -> "Set<Any>"

            packageName == "kotlin.collections" && className in
                listOf(
                    "Map",
                    "MutableMap",
                )
            -> "Map<Any, Any>"

            packageName == "kotlin.collections" && className == "Collection" -> "Collection<Any>"
            packageName == "kotlin" && className == "Result" -> "Result<Any>"
            packageName == "kotlin" && className == "Array" -> "Array<Any>"
            else -> className
        }
    }

    /**
     * Converts type arguments to string representation.
     * Extracted to reduce complexity of handleGenericType().
     *
     * @param arguments List of type arguments to convert
     * @param preserveTypeParameters Whether to preserve generic type parameters
     * @return String representation like "<T, R>" or empty string
     */
    private fun typeArgumentsToString(
        arguments: List<IrTypeArgument>,
        preserveTypeParameters: Boolean,
    ): String {
        if (arguments.isEmpty()) return ""

        val typeArgs =
            arguments.map { arg ->
                when (arg) {
                    is IrTypeProjection -> irTypeToKotlinString(arg.type, preserveTypeParameters)
                    else -> "Any"
                }
            }
        return "<${typeArgs.joinToString(", ")}>"
    }

    /**
     * Generates default values for function types.
     */
    private fun generateFunctionDefault(irType: IrType): String {
        val returnType =
            if (irType is IrSimpleType && irType.arguments.isNotEmpty()) {
                val lastArg = irType.arguments.last()
                if (lastArg is IrTypeProjection) {
                    getDefaultValue(lastArg.type)
                } else {
                    "Unit"
                }
            } else {
                "Unit"
            }

        return "{ $returnType }"
    }

    /**
     * Handles default values for class types with intelligent defaults.
     */
    private fun handleClassDefault(irType: IrType): String {
        val irClass = irType.getClass() ?: return "TODO(\"Unknown type\")"
        val className = getSimpleClassName(irClass)
        val packageName = irClass.kotlinFqName.parent().asString()

        return getCollectionDefault(packageName, className)
            ?: getKotlinStdlibDefault(packageName, className)
            ?: when (irClass.kind) {
                org.jetbrains.kotlin.descriptors.ClassKind.CLASS -> "null"
                org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE -> "null"
                org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS -> handleEnumDefault(irClass, className)
                else -> "TODO(\"Implement default for $className\")"
            }
    }

    /**
     * Returns default value for Kotlin stdlib types (Result, Array, Pair, Triple).
     *
     * @param packageName The package name of the type
     * @param className The simple class name
     * @return Default value or null if not a stdlib type
     */
    private fun getKotlinStdlibDefault(
        packageName: String,
        className: String,
    ): String? {
        if (packageName != "kotlin") return null

        return when {
            className == "Result" -> "Result.success(Any())"
            className == "Array" -> "emptyArray()"
            className == "Pair" -> "Pair(null, null)"
            className == "Triple" -> "Triple(null, null, null)"
            className.startsWith("Function") -> "{ TODO(\"Function not implemented\") }"
            else -> null
        }
    }

    /**
     * Returns default value for Kotlin collection types.
     *
     * @param packageName The package name of the type
     * @param className The simple class name
     * @return Default value or null if not a collection
     */
    private fun getCollectionDefault(
        packageName: String,
        className: String,
    ): String? {
        if (packageName != "kotlin.collections") return null

        return when (className) {
            "List", "MutableList" -> "emptyList()"
            "Set", "MutableSet" -> "emptySet()"
            "Map", "MutableMap" -> "emptyMap()"
            "Collection" -> "emptyList()"
            else -> null
        }
    }

    /**
     * Handles enum default value generation.
     */
    private fun handleEnumDefault(
        irClass: IrClass,
        className: String,
    ): String {
        val enumEntries = irClass.declarations.filterIsInstance<IrEnumEntry>()
        return if (enumEntries.isNotEmpty()) {
            "$className.${enumEntries.first().name.asString()}"
        } else {
            "TODO(\"Empty enum $className\")"
        }
    }

    /**
     * Gets simple class name from FQ name, avoiding package qualification.
     */
    private fun getSimpleClassName(irClass: IrClass): String = irClass.name.asString()

    /**
     * Check if a type is primitive and doesn't need imports.
     */
    fun isPrimitiveType(irType: IrType): Boolean =
        irType.isString() || irType.isInt() || irType.isBoolean() ||
            irType.isUnit() || irType.isLong() || irType.isFloat() ||
            irType.isDouble() || irType.isChar() || irType.isByte() ||
            irType.isShort()

    /**
     * Checks if an IR type represents a function type.
     */
    private fun isFunction(irType: IrType): Boolean {
        val irClass = irType.getClass() ?: return false
        val className = irClass.name.asString()
        val packageName = irClass.kotlinFqName.parent().asString()
        return packageName == "kotlin" && className.startsWith("Function")
    }

    /**
     * Checks if an IR type represents a suspend function type.
     */
    private fun isSuspendFunction(irType: IrType): Boolean {
        val irClass = irType.getClass() ?: return false
        val className = irClass.name.asString()
        val packageName = irClass.kotlinFqName.parent().asString()
        return packageName == "kotlin.coroutines" && className.startsWith("SuspendFunction")
    }
}
