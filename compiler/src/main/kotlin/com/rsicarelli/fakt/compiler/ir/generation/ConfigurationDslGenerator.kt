// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.model.CodeClass
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.compiler.core.types.TypeResolution
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import com.rsicarelli.fakt.compiler.ir.analysis.AnnotationAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ParameterAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis

/**
 * Generates configuration DSL classes for fake implementations.
 *
 * The config class acts as a standalone builder — it collects behavior lambdas internally during DSL
 * configuration, then the factory function reads these behaviors to construct an immutable fake.
 * The config class does NOT hold a reference to the fake implementation.
 *
 * Uses the type-safe CodeFile DSL for clean, composable code generation.
 */
internal class ConfigurationDslGenerator(private val typeResolver: TypeResolution) {
    companion object {
        /** Length of "Array<" prefix when extracting generic type from Array<T>. */
        private const val ARRAY_PREFIX_LENGTH = 6
    }

    /**
     * Generates a configuration DSL CodeFile for the fake implementation.
     *
     * Uses type-safe DSL for clean, composable code generation. The returned CodeFile contains only
     * the class declaration (no package/imports), suitable for being added to an existing file.
     *
     * @param analysis The analyzed interface metadata
     * @param fakeClassName The name of the fake implementation class (unused, kept for API compat)
     * @return CodeFile containing the configuration DSL class
     */
    fun generateConfigurationDslCodeFile(
        analysis: InterfaceAnalysis,
        @Suppress("UNUSED_PARAMETER") fakeClassName: String,
    ): CodeFile {
        val configClassName = "Fake${analysis.interfaceName}Config"
        val (typeParamsForHeader, whereClause) =
            formatTypeParametersWithWhereClause(analysis.typeParameters)
        val propagatedAnnotations = extractPropagatedAnnotations(analysis.annotations)

        return codeFile("") {
            klass(configClassName) {
                // Apply visibility
                applyVisibility(analysis.visibility)

                // Add type parameters
                addTypeParameters(typeParamsForHeader)

                // Add where clause if needed
                if (whereClause.isNotEmpty()) {
                    where(whereClause)
                }

                // Add propagated annotations
                propagatedAnnotations.forEach { annotation ->
                    annotation(annotation.simpleName, annotation.renderedArguments)
                }

                // Generate internal behavior properties + DSL configurator methods
                analysis.functions.forEach { func ->
                    generateFunctionBehaviorProperty(func)
                    generateFunctionConfigurator(func, analysis.visibility)
                }

                analysis.properties.forEach { prop ->
                    generatePropertyBehaviorProperty(prop)
                    generatePropertyConfigurator(prop, analysis.visibility)
                }
            }
        }
    }

    /**
     * Generates a configuration DSL class for the fake implementation.
     *
     * This is the legacy string-based API that delegates to the DSL-based implementation and
     * renders the result to a string.
     *
     * @param analysis The analyzed interface metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated configuration DSL class code
     */
    fun generateConfigurationDsl(analysis: InterfaceAnalysis, fakeClassName: String): String {
        val codeFile = generateConfigurationDslCodeFile(analysis, fakeClassName)

        // Extract the class declaration and render it
        val klass = codeFile.declarations.firstOrNull() as? CodeClass ?: return ""

        val builder = CodeBuilder()
        klass.renderTo(builder)
        return builder.build().trimEnd()
    }

    /**
     * Generates a configuration DSL CodeFile for the fake class implementation.
     *
     * @param analysis The analyzed class metadata
     * @param fakeClassName The name of the fake implementation class (unused, kept for API compat)
     * @return CodeFile containing the configuration DSL class
     */
    fun generateConfigurationDslCodeFile(
        analysis: ClassAnalysis,
        @Suppress("UNUSED_PARAMETER") fakeClassName: String,
    ): CodeFile {
        val configClassName = "Fake${analysis.className}Config"
        val (typeParamsForHeader, whereClause) =
            formatTypeParametersWithWhereClause(analysis.typeParameters)
        val propagatedAnnotations = extractPropagatedAnnotations(analysis.annotations)

        return codeFile("") {
            klass(configClassName) {
                // Apply visibility
                applyVisibility(analysis.visibility)

                // Add type parameters
                addTypeParameters(typeParamsForHeader)

                // Add where clause if needed
                if (whereClause.isNotEmpty()) {
                    where(whereClause)
                }

                // Add propagated annotations
                propagatedAnnotations.forEach { annotation ->
                    annotation(annotation.simpleName, annotation.renderedArguments)
                }

                // Generate configuration methods for abstract methods
                analysis.abstractMethods.forEach { func ->
                    generateFunctionBehaviorProperty(func)
                    generateFunctionConfigurator(func, analysis.visibility)
                }

                // Generate configuration methods for open methods
                analysis.openMethods.forEach { func ->
                    generateFunctionBehaviorProperty(func)
                    generateFunctionConfigurator(func, analysis.visibility)
                }

                // Generate configuration methods for abstract properties
                analysis.abstractProperties.forEach { prop ->
                    generatePropertyBehaviorProperty(prop)
                    generatePropertyConfigurator(prop, analysis.visibility)
                }

                // Generate configuration methods for open properties
                analysis.openProperties.forEach { prop ->
                    generatePropertyBehaviorProperty(prop)
                    generatePropertyConfigurator(prop, analysis.visibility)
                }
            }
        }
    }

    /**
     * Generates a configuration DSL class for the fake class implementation.
     *
     * This is the legacy string-based API that delegates to the DSL-based implementation.
     *
     * @param analysis The analyzed class metadata
     * @param fakeClassName The name of the fake implementation class
     * @return The generated configuration DSL class code
     */
    fun generateConfigurationDsl(analysis: ClassAnalysis, fakeClassName: String): String {
        val codeFile = generateConfigurationDslCodeFile(analysis, fakeClassName)

        // Extract the class declaration and render it
        val klass = codeFile.declarations.firstOrNull() as? CodeClass ?: return ""

        val builder = CodeBuilder()
        klass.renderTo(builder)
        return builder.build().trimEnd()
    }

    /** Applies visibility modifier to the class builder. */
    private fun ClassBuilder.applyVisibility(visibility: FirVisibility) {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            else -> public()
        }
    }

    /** Adds type parameters to the class builder. */
    private fun ClassBuilder.addTypeParameters(typeParamsForHeader: List<String>) {
        typeParamsForHeader.forEach { param ->
            val parts = param.split(" : ", limit = 2)
            val name = parts[0].trim()
            val constraint = if (parts.size > 1) parts[1].trim() else null
            if (constraint != null) {
                typeParam(name, constraint)
            } else {
                typeParam(name)
            }
        }
    }

    /**
     * Generates an internal mutable behavior property for a function.
     *
     * The config class stores behaviors as `internal var` properties during the DSL phase. These
     * are read by the factory function to construct an immutable fake.
     */
    private fun ClassBuilder.generateFunctionBehaviorProperty(function: FunctionAnalysis) {
        val behaviorSignature = buildBehaviorSignature(function)
        property("${function.name}Behavior", behaviorSignature) {
            internal()
            mutable()
            initializer = "null"
        }
    }

    /**
     * Generates an internal mutable behavior property for a property.
     *
     * Stores the getter (and optionally setter) behavior during DSL configuration.
     */
    private fun ClassBuilder.generatePropertyBehaviorProperty(property: PropertyAnalysis) {
        val propertyType =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)

        property("${property.name}Behavior", "(() -> $propertyType)?") {
            internal()
            mutable()
            initializer = "null"
        }

        if (property.isMutable) {
            property("set${property.name.replaceFirstChar { it.uppercase() }}Behavior", "((${propertyType}) -> Unit)?") {
                internal()
                mutable()
                initializer = "null"
            }
        }
    }

    /**
     * Generates a function configurator method.
     *
     * DSL method that stores the behavior lambda in the config's internal property.
     */
    private fun ClassBuilder.generateFunctionConfigurator(
        function: FunctionAnalysis,
        visibility: FirVisibility,
    ) {
        val functionName = function.name

        // Build behavior signature
        val behaviorSignature = buildBehaviorSignature(function)
        // Remove the nullable wrapper for the parameter type
        val paramSignature = behaviorSignature.removeSuffix(")?") + ")"

        function(functionName) {
            // Apply visibility
            when (visibility) {
                FirVisibility.PUBLIC -> public()
                FirVisibility.INTERNAL -> internal()
                else -> public()
            }

            // Add type parameters if present
            function.typeParameters.forEach { tp ->
                val parts = tp.split(" : ", limit = 2)
                val name = parts[0].trim()
                val constraint = if (parts.size > 1) parts[1].trim() else null
                if (constraint != null) {
                    typeParam(name, constraint)
                } else {
                    typeParam(name)
                }
            }

            parameter("behavior", paramSignature)
            returns("Unit")
            expressionBody = "run { ${functionName}Behavior = behavior }"
        }
    }

    /**
     * Generates a property configurator method.
     *
     * DSL method that stores the behavior lambda in the config's internal property.
     */
    private fun ClassBuilder.generatePropertyConfigurator(
        property: PropertyAnalysis,
        visibility: FirVisibility,
    ) {
        val propertyName = property.name
        val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }
        val propertyType =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)

        // Getter configuration
        function(propertyName) {
            when (visibility) {
                FirVisibility.PUBLIC -> public()
                FirVisibility.INTERNAL -> internal()
                else -> public()
            }

            parameter("behavior", "() -> $propertyType")
            returns("Unit")
            expressionBody = "run { ${propertyName}Behavior = behavior }"
        }

        // Setter configuration for mutable properties
        if (property.isMutable) {
            function("set$capitalizedName") {
                when (visibility) {
                    FirVisibility.PUBLIC -> public()
                    FirVisibility.INTERNAL -> internal()
                    else -> public()
                }

                parameter("behavior", "($propertyType) -> Unit")
                returns("Unit")
                expressionBody = "run { set${capitalizedName}Behavior = behavior }"
            }
        }
    }

    /**
     * Builds the behavior signature for a function.
     *
     * Returns a nullable function type for use as internal config property.
     */
    private fun buildBehaviorSignature(function: FunctionAnalysis): String {
        val suspendModifier = if (function.isSuspend) "suspend " else ""

        // Keep original parameter types (including method-level generics)
        val regularParamTypes =
            function.parameters.joinToString(", ") { param ->
                if (param.isVararg) {
                    val elementType = unwrapVarargsType(param)
                    "Array<out $elementType>"
                } else {
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }
            }

        // For extension functions, prepend receiver type to parameter list
        val parameterTypes =
            if (function.extensionReceiverType != null) {
                val receiverTypeStr =
                    typeResolver.irTypeToKotlinString(
                        function.extensionReceiverType,
                        preserveTypeParameters = true,
                    )
                if (regularParamTypes.isEmpty()) {
                    receiverTypeStr
                } else {
                    "$receiverTypeStr, $regularParamTypes"
                }
            } else {
                regularParamTypes
            }

        // Keep original return type (including method-level generics)
        val returnType =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)

        val baseType =
            if (parameterTypes.isEmpty()) {
                "$suspendModifier() -> $returnType"
            } else {
                "$suspendModifier($parameterTypes) -> $returnType"
            }

        return "($baseType)?"
    }

    /**
     * Extracts annotations that need to be propagated to the config class.
     *
     * This includes:
     * - @OptIn: Required because the class references types that require opt-in
     * - @Deprecated: Propagated so that deprecated types have deprecated config classes
     *
     * Excludes opt-in markers because fakes should be freely usable in tests.
     */
    private fun extractPropagatedAnnotations(
        annotations: List<AnnotationAnalysis>
    ): List<AnnotationAnalysis> =
        annotations.filter {
            (it.simpleName == "OptIn" || it.simpleName == "Deprecated") && !it.isOptInMarker
        }

    /**
     * Unwraps varargs Array<T> to element type T.
     *
     * @param param The varargs parameter
     * @return The unwrapped element type
     */
    private fun unwrapVarargsType(param: ParameterAnalysis): String {
        val arrayType = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
        return if (arrayType.startsWith("Array<") && arrayType.endsWith(">")) {
            arrayType.substring(ARRAY_PREFIX_LENGTH, arrayType.length - 1)
        } else {
            "String" // Safe fallback for varargs
        }
    }

    /**
     * Formats type parameters for config class headers, handling where clauses for multiple
     * constraints.
     */
    private fun formatTypeParametersWithWhereClause(
        typeParameters: List<String>
    ): Pair<List<String>, String> {
        if (typeParameters.isEmpty()) {
            return emptyList<String>() to ""
        }

        val paramsForHeader = mutableListOf<String>()
        val whereClauses = mutableListOf<String>()

        for (typeParam in typeParameters) {
            val colonIndex = typeParam.indexOf(" :")
            if (colonIndex == -1) {
                paramsForHeader.add(typeParam)
                continue
            }

            val name = typeParam.substring(0, colonIndex).trim()
            val constraints = typeParam.substring(colonIndex + 2).trim()
            val constraintList = constraints.split(",").map { it.trim() }

            if (constraintList.size == 1) {
                paramsForHeader.add(typeParam)
            } else {
                paramsForHeader.add(name)
                constraintList.forEach { constraint -> whereClauses.add("$name : $constraint") }
            }
        }

        return paramsForHeader to whereClauses.joinToString(", ")
    }

    /** Extracts type parameter names as a type argument string. */
    private fun extractTypeParameterNames(typeParameters: List<String>): String =
        if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { it.substringBefore(" :").trim() }}>"
        } else {
            ""
        }
}
