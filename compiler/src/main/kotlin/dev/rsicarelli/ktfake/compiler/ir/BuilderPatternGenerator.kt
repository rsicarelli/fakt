// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Generator for builder pattern implementation in IR phase.
 *
 * Creates builder classes for data classes marked with @Fake(builder = true).
 * Handles default value generation, nested fakes, and configuration DSL integration.
 */
class BuilderPatternGenerator {

    private val defaultValueGenerator = DefaultValueGenerator()

    /**
     * Generate builder class for a data class.
     */
    fun generateBuilderClass(className: String, properties: List<String>): String {
        val builderClassName = "Fake${className}Builder"
        val propertyDeclarations = generateBuilderProperties(properties)
        val buildMethod = generateBuildMethod(className, properties)

        return """
        internal class $builderClassName {
            $propertyDeclarations

            $buildMethod
        }
        """.trimIndent()
    }

    /**
     * Generate default values for builder properties.
     */
    fun generateDefaultValues(className: String, properties: List<String>): String {
        return properties.joinToString("\n    ") { property ->
            val (name, type) = parseProperty(property)
            val defaultValue = defaultValueGenerator.generateDefaultValue(type, name)
            "private var $name: $type = $defaultValue"
        }
    }

    /**
     * Generate builder with nested fake support.
     */
    fun generateBuilderWithNestedFakes(className: String, properties: List<String>): String {
        val builderClassName = "Fake${className}Builder"
        val propertyDeclarations = properties.joinToString("\n    ") { property ->
            val (name, type) = parseProperty(property)
            val defaultValue = if (isCustomType(type)) {
                generateNestedFakeCall(type)
            } else {
                defaultValueGenerator.generateDefaultValue(type, name)
            }
            "private var $name: $type = $defaultValue"
        }

        return """
        internal class $builderClassName {
            $propertyDeclarations

            fun build(): $className = $className(${properties.joinToString(", ") { parseProperty(it).first }})
        }
        """.trimIndent()
    }

    /**
     * Generate factory function with builder pattern support.
     */
    fun generateBuilderFactoryFunction(className: String): String {
        return """
        fun fake$className(configure: Fake${className}Config.() -> Unit = {}): $className {
            return Fake${className}Builder().apply {
                Fake${className}Config(this).configure()
            }.build()
        }
        """.trimIndent()
    }

    /**
     * Generate configuration DSL for builder pattern.
     */
    fun generateBuilderConfigurationDsl(className: String, properties: List<String>): String {
        val configClassName = "Fake${className}Config"
        val propertySetters = properties.joinToString("\n    ") { property ->
            val (name, type) = parseProperty(property)
            """
            var $name: $type
                get() = builder.$name
                set(value) { builder.$name = value }
            """.trimIndent()
        }

        return """
        class $configClassName(private val builder: Fake${className}Builder) {
            $propertySetters
        }
        """.trimIndent()
    }

    /**
     * Generate unique string default with UUID.
     */
    fun generateUniqueStringDefault(className: String): String {
        return defaultValueGenerator.generateUniqueString(className.lowercase())
    }

    /**
     * Detect which property types need nested fakes.
     */
    fun detectNestedTypes(properties: List<String>): Set<String> {
        return properties.mapNotNull { property ->
            val (_, type) = parseProperty(property)
            val typeInfo = defaultValueGenerator.parseTypeInfo(type)

            if (isCustomType(typeInfo.baseType)) {
                typeInfo.baseType
            } else {
                // Extract generic parameters that might be custom types
                typeInfo.genericParams.filter { isCustomType(it) }
            }.let { if (it is List<*>) it.filterIsInstance<String>().toSet() else setOf(it as String) }
        }.flatten().toSet()
    }

    /**
     * Generate builder properties with default values.
     */
    private fun generateBuilderProperties(properties: List<String>): String {
        return properties.joinToString("\n    ") { property ->
            val (name, type) = parseProperty(property)
            val defaultValue = defaultValueGenerator.generateDefaultValue(type, name)
            "private var $name: $type = $defaultValue"
        }
    }

    /**
     * Generate build method that creates the data class instance.
     */
    private fun generateBuildMethod(className: String, properties: List<String>): String {
        val constructorArgs = properties.joinToString(", ") { property ->
            parseProperty(property).first
        }

        return "fun build(): $className = $className($constructorArgs)"
    }

    /**
     * Generate nested fake function call.
     */
    private fun generateNestedFakeCall(typeName: String): String {
        val functionName = "fake${typeName.replaceFirstChar { it.titlecase() }}"
        return "$functionName()"
    }

    /**
     * Check if a type is a custom class (not a primitive or standard library type).
     */
    private fun isCustomType(type: String): Boolean {
        val primitives = setOf(
            "String", "Int", "Long", "Double", "Float", "Boolean", "Char", "Byte", "Short",
            "List", "Set", "Map", "Array", "MutableList", "MutableSet", "MutableMap"
        )
        return type !in primitives && !type.endsWith("?")
    }

    /**
     * Parse property string into name and type.
     */
    private fun parseProperty(property: String): Pair<String, String> {
        val parts = property.split(":")
        return parts[0].trim() to parts[1].trim()
    }
}
