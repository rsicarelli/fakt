// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.uuid.ExperimentalUuidApi

/**
 * Generator for default values in builder pattern.
 *
 * Creates sensible and unique default values for different property types.
 * Uses official Kotlin UUID library for cross-platform unique string generation,
 * type-specific defaults, and nested fakes.
 */
class DefaultValueGenerator {

    /**
     * Type information parsed from property type string.
     */
    data class TypeInfo(
        val baseType: String,
        val isNullable: Boolean = false,
        val genericParams: List<String> = emptyList()
    )

    /**
     * Generate default value for a property type.
     */
    fun generateDefaultValue(
        type: String,
        propertyName: String,
        enumValues: List<String> = emptyList()
    ): String {
        val typeInfo = parseTypeInfo(type)

        if (typeInfo.isNullable) {
            return "null"
        }

        return when (typeInfo.baseType) {
            "String" -> generateUniqueString(propertyName)
            "Int" -> "0"
            "Long" -> "0L"
            "Double" -> "0.0"
            "Float" -> "0.0f"
            "Boolean" -> "true"
            "List" -> "emptyList()"
            "Set" -> "emptySet()"
            "Map" -> "emptyMap()"
            else -> {
                if (enumValues.isNotEmpty()) {
                    "${typeInfo.baseType}.${enumValues.first()}"
                } else {
                    generateNestedFakeCall(typeInfo.baseType)
                }
            }
        }
    }

    /**
     * Generate context-aware default values based on property name.
     */
    fun generateContextAwareDefault(type: String, propertyName: String): String {
        return when {
            propertyName.lowercase().contains("email") -> "\"fake.email@example.com\""
            propertyName.lowercase().contains("name") -> "\"Fake ${propertyName.replaceFirstChar { it.titlecase() }}\""
            propertyName.lowercase().contains("age") && type == "Int" -> "25"
            else -> generateDefaultValue(type, propertyName)
        }
    }

    /**
     * Generate unique string with official Kotlin UUID.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun generateUniqueString(contextualName: String): String {
        val uuid = kotlin.uuid.Uuid.random().toString()
        return "\"fake_${contextualName}_$uuid\""
    }

    /**
     * Generate nested fake function call for custom types.
     */
    private fun generateNestedFakeCall(typeName: String): String {
        val functionName = "fake${typeName}"
        return "$functionName()"
    }

    /**
     * Parse type information from type string.
     */
    fun parseTypeInfo(type: String): TypeInfo {
        val cleanType = type.trim()
        val isNullable = cleanType.endsWith("?")
        val baseType = if (isNullable) cleanType.dropLast(1) else cleanType

        // Handle generic types like List<String>
        if (baseType.contains("<")) {
            val baseTypeName = baseType.substringBefore("<")
            val genericPart = baseType.substringAfter("<").substringBeforeLast(">")
            val genericParams = genericPart.split(",").map { it.trim() }

            return TypeInfo(baseTypeName, isNullable, genericParams)
        }

        return TypeInfo(baseType, isNullable)
    }
}
