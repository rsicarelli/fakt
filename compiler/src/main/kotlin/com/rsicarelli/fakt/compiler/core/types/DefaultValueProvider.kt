// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
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
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Provides appropriate default values for IR types.
 *
 * Handles:
 * - Primitive type defaults (0, false, "", etc.)
 * - Nullable type defaults (null)
 * - Collection defaults (emptyList(), emptyMap(), etc.)
 * - Function defaults ({ ... })
 * - Enum defaults (EnumName.FIRST_ENTRY)
 * - Complex type defaults (null or intelligent defaults)
 */
internal class DefaultValueProvider(
    private val functionTypeHandler: FunctionTypeHandler,
) {
    /**
     * Generates appropriate default value for an IR type.
     *
     * @param irType The type to generate a default value for
     * @return String representation of the default value
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun provide(irType: IrType): String =
        getPrimitiveDefault(irType)
            ?: when {
                irType.isMarkedNullable() -> "null"
                irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> "Any()"
                functionTypeHandler.isFunction(irType) ||
                    functionTypeHandler.isSuspendFunction(irType) -> generateFunctionDefault(irType)

                else -> handleClassDefault(irType)
            }

    /**
     * Returns default value for primitive types, or null if not a primitive.
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
            irType.isNothing() -> "error(\"Nothing type has no values - this indicates a type error\") as Nothing"
            irType.isAny() -> "Any()"
            else -> null
        }

    /**
     * Generates default values for function types.
     */
    private fun generateFunctionDefault(irType: IrType): String {
        val returnType =
            if (irType is IrSimpleType && irType.arguments.isNotEmpty()) {
                val lastArg = irType.arguments.last()
                if (lastArg is IrTypeProjection) {
                    provide(lastArg.type)
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
        val irClass =
            irType.getClass()
                ?: return "error(\"Unknown type requires explicit configuration. Configure behavior via fake factory DSL.\") as Nothing"
        val className = irClass.name.asString()
        val packageName = irClass.kotlinFqName.parent().asString()

        return getCollectionDefault(packageName, className)
            ?: getKotlinStdlibDefault(packageName, className)
            ?: when (irClass.kind) {
                ClassKind.CLASS -> "null"
                ClassKind.INTERFACE -> "null"
                ClassKind.ENUM_CLASS -> handleEnumDefault(irClass, className)
                else ->
                    "error(\"Type '$className' requires explicit configuration. " +
                        "Fakt prioritizes type-safety over auto-mocking. " +
                        "Configure behavior via fake factory DSL.\") as Nothing"
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
            className.startsWith(
                "Function",
            ) -> "{ error(\"Function type requires explicit configuration. Configure behavior via fake factory DSL.\") }"
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
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun handleEnumDefault(
        irClass: IrClass,
        className: String,
    ): String {
        val enumEntries = irClass.declarations.filterIsInstance<IrEnumEntry>()
        return if (enumEntries.isNotEmpty()) {
            "$className.${enumEntries.first().name.asString()}"
        } else {
            "error(\"Empty enum '$className' has no values - this indicates a type error\") as Nothing"
        }
    }
}
