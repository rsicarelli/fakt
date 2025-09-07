// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Integrator for builder pattern with existing fake generation infrastructure.
 *
 * Coordinates builder pattern generation with call tracking, factory functions,
 * and existing IR generation systems.
 */
class BuilderPatternIntegrator(
    private val builderGenerator: BuilderPatternGenerator
) {

    /**
     * Generate data class implementation with optional builder pattern.
     */
    fun generateDataClassImplementation(
        className: String,
        properties: List<String>,
        useBuilder: Boolean
    ): String {
        if (!useBuilder) {
            return generateSimpleFactory(className)
        }

        val builderClass = builderGenerator.generateBuilderClass(className, properties)
        val configDsl = builderGenerator.generateBuilderConfigurationDsl(className, properties)
        val factoryFunction = builderGenerator.generateBuilderFactoryFunction(className)

        return """
        $builderClass

        $configDsl

        $factoryFunction
        """.trimIndent()
    }

    /**
     * Generate data class implementation with call tracking support.
     */
    fun generateDataClassWithCallTracking(
        className: String,
        properties: List<String>,
        useBuilder: Boolean,
        trackCalls: Boolean
    ): String {
        val baseImplementation = generateDataClassImplementation(className, properties, useBuilder)

        if (!trackCalls) {
            return baseImplementation
        }

        val callTrackingCode = """
        // Call tracking for $className
        data class ${className}Call(val instance: $className)
        internal val ${className.lowercase()}Calls = mutableListOf<${className}Call>()

        internal fun verify${className}Tracked(times: Int = 1): Boolean =
            ${className.lowercase()}Calls.size == times
        """.trimIndent()

        return """
        $baseImplementation

        $callTrackingCode
        """.trimIndent()
    }

    /**
     * Generate builders for nested data class structures.
     */
    fun generateNestedDataClassBuilders(
        mainClass: String,
        mainProperties: List<String>,
        nestedClasses: Map<String, List<String>>
    ): String {
        val mainBuilder = builderGenerator.generateBuilderWithNestedFakes(mainClass, mainProperties)

        val nestedBuilders = nestedClasses.map { (className, properties) ->
            builderGenerator.generateBuilderWithNestedFakes(className, properties)
        }.joinToString("\n\n")

        return """
        $mainBuilder

        $nestedBuilders
        """.trimIndent()
    }

    /**
     * Integrate builder pattern with existing factory generation.
     */
    fun integrateWithFactoryGeneration(className: String): String {
        return """
        // Integration with existing factory generation for $className
        // Maintains thread-safe factory function patterns
        fun fake$className(configure: Fake${className}Config.() -> Unit = {}): $className {
            return FakeRuntime.getOrCreate("$className") {
                Fake${className}Builder().apply {
                    Fake${className}Config(this).configure()
                }.build()
            }
        }
        """.trimIndent()
    }

    /**
     * Generate builder configuration API.
     */
    fun generateBuilderConfigurationApi(className: String, properties: List<String>): String {
        return builderGenerator.generateBuilderConfigurationDsl(className, properties)
    }

    /**
     * Generate builder with circular dependency handling.
     */
    fun generateBuilderWithCircularDependencyHandling(
        className: String,
        properties: List<String>,
        circularTypes: Set<String>
    ): String {
        val builderClassName = "Fake${className}Builder"

        val propertyDeclarations = properties.joinToString("\n    ") { property ->
            val (name, type) = parseProperty(property)
            if (type in circularTypes) {
                "private val $name: $type by lazy { fake${type}() }"
            } else {
                val defaultValue = builderGenerator.generateUniqueStringDefault(className)
                "private var $name: $type = $defaultValue"
            }
        }

        return """
        internal class $builderClassName {
            $propertyDeclarations

            fun build(): $className = $className(${properties.joinToString(", ") { parseProperty(it).first }})
        }
        """.trimIndent()
    }

    /**
     * Generate optimized builder for performance-critical scenarios.
     */
    fun generateOptimizedBuilder(className: String, properties: List<String>): String {
        val builderClass = builderGenerator.generateBuilderClass(className, properties)

        return """
        // Performance-optimized builder for $className
        inline fun optimizedFake$className(): $className = $className(/* inline optimizations */)

        $builderClass
        """.trimIndent()
    }

    /**
     * Generate builders for multiple classes in batch.
     */
    fun generateBuildersForMultipleClasses(
        dataClasses: Map<String, List<String>>
    ): String {
        return dataClasses.map { (className, properties) ->
            val builder = builderGenerator.generateBuilderWithNestedFakes(className, properties)
            val factoryFunction = builderGenerator.generateBuilderFactoryFunction(className)

            """
            $builder

            $factoryFunction
            """.trimIndent()
        }.joinToString("\n\n")
    }

    /**
     * Generate simple factory function without builder pattern.
     */
    private fun generateSimpleFactory(className: String): String {
        return """
        fun fake$className(): $className {
            return $className(/* default values */)
        }
        """.trimIndent()
    }

    /**
     * Parse property string into name and type.
     */
    private fun parseProperty(property: String): Pair<String, String> {
        val parts = property.split(":")
        return parts[0].trim() to parts[1].trim()
    }
}
