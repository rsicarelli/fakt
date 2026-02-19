// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.builder.parseType
import com.rsicarelli.fakt.codegen.extensions.eraseTypeParamsToAny
import com.rsicarelli.fakt.codegen.extensions.typeContainsAnyParam
import com.rsicarelli.fakt.codegen.model.CodeClass
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.render
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.codegen.strategy.DefaultValueResolver
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
 * The config class acts as a standalone builder — it collects behavior lambdas internally during
 * DSL configuration, then the factory function reads these behaviors to construct an immutable
 * fake. The config class does NOT hold a reference to the fake implementation.
 *
 * Uses the type-safe CodeFile DSL for clean, composable code generation.
 */
internal class ConfigurationDslGenerator(private val typeResolver: TypeResolution) {
    companion object {
        /** Length of "Array<" prefix when extracting generic type from Array<T>. */
        private const val ARRAY_PREFIX_LENGTH = 6
    }

    private val defaultValueResolver = DefaultValueResolver()

    /** Extracts method-level type parameter names from a function analysis. */
    private fun extractMethodTypeParamNames(function: FunctionAnalysis): Set<String> =
        function.typeParameters.map { it.split(" : ", limit = 2)[0].trim() }.toSet()

    /** Extracts class-level type parameter names from type parameter declarations. */
    private fun extractClassTypeParamNames(typeParameters: List<String>): Set<String> =
        typeParameters.map { it.split(" : ", limit = 2)[0].trim() }.toSet()

    /**
     * Detects single-param identity pattern: param type == return type → `{ it }`.
     *
     * Extension functions are excluded because the behavior lambda requires TWO parameters.
     */
    private fun shouldUseIdentityDefault(function: FunctionAnalysis): Boolean {
        if (function.extensionReceiverType != null || function.parameters.size != 1) return false
        val paramType =
            typeResolver
                .irTypeToKotlinString(function.parameters[0].type, preserveTypeParameters = true)
                .replace("?", "")
                .trim()
        val returnType =
            typeResolver
                .irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
                .replace("?", "")
                .trim()
        return paramType == returnType
    }

    /**
     * Detects function invocation pattern: `fun <T> m(f: () -> T): T` → `{ p0 -> p0() }`.
     *
     * Strict pattern requires:
     * - Method has type parameters and exactly one parameter
     * - Parameter is a zero-arg function type `() -> T` or `suspend () -> T`
     * - Method return type exactly matches the function's return type
     */
    private fun detectFunctionInvocationDefault(function: FunctionAnalysis): Boolean {
        if (function.typeParameters.isEmpty() || function.parameters.size != 1) return false
        val typeParamNames = extractMethodTypeParamNames(function)
        val returnType =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        if (!typeContainsAnyParam(returnType, typeParamNames)) return false
        val paramType =
            typeResolver.irTypeToKotlinString(
                function.parameters[0].type,
                preserveTypeParameters = true,
            )
        if (!paramType.contains("->")) return false
        val funcSignature = paramType.replace("suspend ", "").trim()
        val beforeArrow = funcSignature.substringBefore("->").trim()
        if (beforeArrow != "()") return false
        val returnPart = funcSignature.substringAfter("->").trim()
        if (!typeContainsAnyParam(returnPart, typeParamNames)) return false
        return returnType.trim() == returnPart.trim()
    }

    /** Builds a method signature string for error messages (matches FakeImpl format). */
    private fun buildMethodSignature(function: FunctionAnalysis): String {
        val params =
            function.parameters.joinToString {
                typeResolver.irTypeToKotlinString(it.type, preserveTypeParameters = true)
            }
        val returnType =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        return "${function.name}($params): $returnType"
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
        fakeClassName: String,
    ): CodeFile {
        val configClassName = "Fake${analysis.interfaceName}Config"
        val typeArgs = extractTypeParameterNames(analysis.typeParameters)
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

                val classTypeParamNames = extractClassTypeParamNames(analysis.typeParameters)

                // Generate internal behavior properties + DSL configurator methods
                analysis.functions.forEach { func ->
                    generateFunctionBehaviorProperty(func, withDefault = true, classTypeParamNames)
                    generateFunctionConfigurator(func, analysis.visibility)
                }

                analysis.properties.forEach { prop ->
                    generatePropertyBehaviorProperty(prop, withDefault = true, classTypeParamNames)
                    generatePropertyConfigurator(prop, analysis.visibility)
                }

                // Generate @PublishedApi build() method
                generateInterfaceBuildMethod(
                    fakeClassName = fakeClassName,
                    typeArgs = typeArgs,
                    functions = analysis.functions,
                    properties = analysis.properties,
                )
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
    fun generateConfigurationDslCodeFile(analysis: ClassAnalysis, fakeClassName: String): CodeFile {
        val configClassName = "Fake${analysis.className}Config"
        val typeArgs = extractTypeParameterNames(analysis.typeParameters)
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

                val classTypeParamNames = extractClassTypeParamNames(analysis.typeParameters)

                // Generate configuration methods for abstract methods
                analysis.abstractMethods.forEach { func ->
                    generateFunctionBehaviorProperty(
                        func,
                        withDefault = true,
                        classTypeParamNames,
                        abstractClassName = analysis.className,
                    )
                    generateFunctionConfigurator(func, analysis.visibility)
                }

                // Generate configuration methods for open methods
                analysis.openMethods.forEach { func ->
                    generateFunctionBehaviorProperty(func, withDefault = false, classTypeParamNames)
                    generateFunctionConfigurator(func, analysis.visibility)
                }

                // Generate configuration methods for abstract properties
                analysis.abstractProperties.forEach { prop ->
                    generatePropertyBehaviorProperty(prop, withDefault = true, classTypeParamNames)
                    generatePropertyConfigurator(prop, analysis.visibility)
                }

                // Generate configuration methods for open properties
                analysis.openProperties.forEach { prop ->
                    generatePropertyBehaviorProperty(prop, withDefault = false, classTypeParamNames)
                    generatePropertyConfigurator(prop, analysis.visibility)
                }

                // Generate @PublishedApi build() method
                generateClassBuildMethod(
                    fakeClassName = fakeClassName,
                    typeArgs = typeArgs,
                    abstractMethods = analysis.abstractMethods,
                    openMethods = analysis.openMethods,
                    abstractProperties = analysis.abstractProperties,
                    openProperties = analysis.openProperties,
                )
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
     *
     * @param withDefault When true, property is non-nullable with a computed default (for
     *   interface/abstract members). When false, property is nullable with null default (for open
     *   members where null means "delegate to super").
     */
    private fun ClassBuilder.generateFunctionBehaviorProperty(
        function: FunctionAnalysis,
        withDefault: Boolean,
        classTypeParamNames: Set<String> = emptySet(),
        abstractClassName: String? = null,
    ) {
        val baseType = buildBaseBehaviorType(function)
        val methodTypeParamNames = extractMethodTypeParamNames(function)

        // For methods with method-level type params, erase to Any? so the property type
        // doesn't reference unresolved type params (e.g., K, V, T).
        val propertyType =
            if (methodTypeParamNames.isNotEmpty()) {
                eraseTypeParamsToAny(baseType, methodTypeParamNames)
            } else {
                baseType
            }

        // Generic methods now get defaults too — their property types are fully erased.
        if (withDefault) {
            val returnTypeStr =
                typeResolver.irTypeToKotlinString(
                    function.returnType,
                    preserveTypeParameters = true,
                )
            val effectiveReturnType =
                if (methodTypeParamNames.isNotEmpty()) {
                    eraseTypeParamsToAny(returnTypeStr, methodTypeParamNames)
                } else {
                    returnTypeStr
                }
            val defaultExpr = computeDefaultExpr(effectiveReturnType, classTypeParamNames)
            val lambdaParams = buildErasedLambdaParams(function, methodTypeParamNames)

            // 4-tier default decision matching FakeImpl:
            // 1. Abstract class method → error(...)
            // 2. Function invocation pattern → { p0 -> p0() }
            // 3. Identity pattern → { it }
            // 4. Fallback → DefaultValueResolver
            val behaviorDefault =
                when {
                    abstractClassName != null -> {
                        val sig = buildMethodSignature(function)
                        val escapedClassName =
                            abstractClassName.replaceFirstChar { it.uppercase() }
                        val errorMsg =
                            "Abstract method '$sig' in class '$abstractClassName' must be configured. " +
                                "Use the DSL: fake$escapedClassName { ${function.name} { ... } }"
                        "{ ${lambdaParams}error(\"$errorMsg\") }"
                    }
                    detectFunctionInvocationDefault(function) -> "{ p0 -> p0() }"
                    shouldUseIdentityDefault(function) -> "{ it }"
                    else -> "{ $lambdaParams$defaultExpr }"
                }

            property("${function.name}Behavior", "($propertyType)") {
                internal()
                mutable()
                initializer = behaviorDefault
            }
        } else {
            property("${function.name}Behavior", "($propertyType)?") {
                internal()
                mutable()
                initializer = "null"
            }
        }
    }

    /**
     * Generates an internal mutable behavior property for a property.
     *
     * Stores the getter (and optionally setter) behavior during DSL configuration.
     *
     * @param withDefault When true, property is non-nullable with a computed default (for
     *   interface/abstract members). When false, property is nullable with null default (for open
     *   members where null means "delegate to super").
     */
    private fun ClassBuilder.generatePropertyBehaviorProperty(
        property: PropertyAnalysis,
        withDefault: Boolean,
        classTypeParamNames: Set<String> = emptySet(),
    ) {
        val propertyType =
            typeResolver.irTypeToKotlinString(property.type, preserveTypeParameters = true)

        if (withDefault) {
            val defaultExpr = computeDefaultExpr(propertyType, classTypeParamNames)
            property("${property.name}Behavior", "(() -> $propertyType)") {
                internal()
                mutable()
                initializer = "{ $defaultExpr }"
            }
        } else {
            property("${property.name}Behavior", "(() -> $propertyType)?") {
                internal()
                mutable()
                initializer = "null"
            }
        }

        if (property.isMutable) {
            if (withDefault) {
                property(
                    "set${property.name.replaceFirstChar { it.uppercase() }}Behavior",
                    "((${propertyType}) -> Unit)",
                ) {
                    internal()
                    mutable()
                    initializer = "{ }"
                }
            } else {
                property(
                    "set${property.name.replaceFirstChar { it.uppercase() }}Behavior",
                    "((${propertyType}) -> Unit)?",
                ) {
                    internal()
                    mutable()
                    initializer = "null"
                }
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
        val methodTypeParamNames = extractMethodTypeParamNames(function)

        // Use the base type directly (non-nullable) as the parameter type
        val paramSignature = "(${buildBaseBehaviorType(function)})"

        function(functionName) {
            // Apply visibility
            when (visibility) {
                FirVisibility.PUBLIC -> public()
                FirVisibility.INTERNAL -> internal()
                else -> public()
            }

            // Generic methods need @Suppress for the bridging casts
            if (methodTypeParamNames.isNotEmpty()) {
                annotation("Suppress", "\"UNCHECKED_CAST\"")
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

            if (methodTypeParamNames.isNotEmpty()) {
                // Build wrapping closure that bridges typed → erased
                val closureBody = buildWrappingClosure(function, methodTypeParamNames)
                body = "${functionName}Behavior = $closureBody"
            } else {
                expressionBody = "run { ${functionName}Behavior = behavior }"
            }
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
     * Builds the base behavior function type for a function (non-nullable).
     *
     * Returns the function type string without nullable wrapper, e.g. `(String) -> Int`.
     */
    private fun buildBaseBehaviorType(function: FunctionAnalysis): String {
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

        return if (parameterTypes.isEmpty()) {
            "$suspendModifier() -> $returnType"
        } else {
            "$suspendModifier($parameterTypes) -> $returnType"
        }
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

    // ==========================================
    // Build Method Generation
    // ==========================================

    /**
     * Generates `@PublishedApi internal fun build()` for interface config classes.
     *
     * All interface behaviors have non-nullable defaults in their config properties, so build()
     * simply passes them through to the FakeImpl constructor.
     */
    private fun ClassBuilder.generateInterfaceBuildMethod(
        fakeClassName: String,
        typeArgs: String,
        functions: List<FunctionAnalysis>,
        properties: List<PropertyAnalysis>,
    ) {
        val args = mutableListOf<String>()
        properties.forEach { addPropertyBuildArg(args = args, prop = it) }
        functions.forEach { addFunctionBuildArg(args = args, func = it) }
        emitBuildFunction(fakeClassName = fakeClassName, typeArgs = typeArgs, args = args)
    }

    /**
     * Generates `@PublishedApi internal fun build()` for class config classes.
     *
     * Both abstract (non-nullable with defaults) and open (nullable, null = delegate to super)
     * behaviors are passed through directly — defaults are already in the config properties.
     */
    private fun ClassBuilder.generateClassBuildMethod(
        fakeClassName: String,
        typeArgs: String,
        abstractMethods: List<FunctionAnalysis>,
        openMethods: List<FunctionAnalysis>,
        abstractProperties: List<PropertyAnalysis>,
        openProperties: List<PropertyAnalysis>,
    ) {
        val args = mutableListOf<String>()
        abstractProperties.forEach { addPropertyBuildArg(args = args, prop = it) }
        openProperties.forEach { addPropertyBuildArg(args = args, prop = it) }
        abstractMethods.forEach { addFunctionBuildArg(args = args, func = it) }
        openMethods.forEach { addFunctionBuildArg(args = args, func = it) }
        emitBuildFunction(fakeClassName = fakeClassName, typeArgs = typeArgs, args = args)
    }

    /** Adds constructor argument(s) for a property behavior to the build args list. */
    private fun addPropertyBuildArg(args: MutableList<String>, prop: PropertyAnalysis) {
        val propType = typeResolver.irTypeToKotlinString(prop.type, preserveTypeParameters = true)
        // StateFlow properties are handled separately (not constructor params)
        if (propType.contains("StateFlow<")) return

        if (prop.isMutable) {
            val cap = prop.name.replaceFirstChar { it.uppercase() }
            args.add("${prop.name}Getter = ${prop.name}Behavior")
            args.add("${prop.name}Setter = set${cap}Behavior")
        } else {
            args.add("${prop.name}Behavior = ${prop.name}Behavior")
        }
    }

    /**
     * Adds a constructor argument for a function behavior to the build args list.
     *
     * All methods (including those with method-level type params) are now included — generic method
     * config properties use erased types (Any?) with non-null defaults, matching the FakeImpl
     * constructor parameter types.
     */
    private fun addFunctionBuildArg(args: MutableList<String>, func: FunctionAnalysis) {
        args.add("${func.name}Behavior = ${func.name}Behavior")
    }

    /** Resolves a default value expression for the given type string. */
    private fun computeDefaultExpr(
        typeStr: String,
        classLevelTypeParams: Set<String> = emptySet(),
    ): String {
        val resolver =
            if (classLevelTypeParams.isNotEmpty()) {
                DefaultValueResolver(classLevelTypeParams)
            } else {
                defaultValueResolver
            }
        return resolver.resolve(type = parseType(typeString = typeStr)).render()
    }

    /** Builds lambda parameter placeholders for a function's default behavior. */
    private fun buildLambdaParams(func: FunctionAnalysis): String {
        val baseParamNames = func.parameters.mapIndexed { i, _ -> "p$i" }
        val paramNames =
            if (func.extensionReceiverType != null) {
                listOf("p_receiver") + baseParamNames
            } else {
                baseParamNames
            }
        return if (paramNames.isEmpty()) "" else "${paramNames.joinToString(", ")} -> "
    }

    /**
     * Builds lambda parameter placeholders for erased default behavior.
     *
     * Same as [buildLambdaParams] but uses underscore-prefixed names to avoid unused warnings when
     * the default simply returns a default value.
     */
    private fun buildErasedLambdaParams(
        func: FunctionAnalysis,
        methodTypeParamNames: Set<String>,
    ): String {
        if (methodTypeParamNames.isEmpty()) return buildLambdaParams(func)

        val baseParamNames = func.parameters.mapIndexed { i, _ -> "_p$i" }
        val paramNames =
            if (func.extensionReceiverType != null) {
                listOf("_p_receiver") + baseParamNames
            } else {
                baseParamNames
            }
        return if (paramNames.isEmpty()) "" else "${paramNames.joinToString(", ")} -> "
    }

    /**
     * Builds a wrapping closure that bridges from typed user lambda to erased property storage.
     *
     * For a method like `fun <K, V> transformMap(map: Map<K, V>, transform: (K, V) -> String):
     * Map<K, String>`, generates:
     * ```
     * { p0, p1 -> behavior(p0 as Map<K, V>, p1 as (K, V) -> String) as Map<Any?, String> }
     * ```
     *
     * The closure's parameters match the erased property type. Inside, it casts erased args back to
     * the typed signature, invokes the user's behavior, then casts the result back to erased.
     */
    private fun buildWrappingClosure(
        function: FunctionAnalysis,
        methodTypeParamNames: Set<String>,
    ): String {
        // Build the list of erased parameter names and their typed cast targets
        val closureParams = mutableListOf<String>()
        val behaviorArgs = mutableListOf<String>()

        // Extension receiver as first parameter
        if (function.extensionReceiverType != null) {
            val receiverType =
                typeResolver.irTypeToKotlinString(
                    function.extensionReceiverType,
                    preserveTypeParameters = true,
                )
            closureParams.add("p_receiver")
            if (typeContainsAnyParam(receiverType, methodTypeParamNames)) {
                behaviorArgs.add("p_receiver as $receiverType")
            } else {
                behaviorArgs.add("p_receiver")
            }
        }

        // Regular parameters
        function.parameters.forEachIndexed { i, param ->
            val paramName = "p$i"
            closureParams.add(paramName)

            val paramType =
                if (param.isVararg) {
                    val arrayType =
                        typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                    "Array<out ${unwrapVarargsType(param)}>"
                } else {
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
                }

            if (typeContainsAnyParam(paramType, methodTypeParamNames)) {
                behaviorArgs.add("$paramName as $paramType")
            } else {
                behaviorArgs.add(paramName)
            }
        }

        // Return type — check if it needs a cast back to erased
        val returnType =
            typeResolver.irTypeToKotlinString(function.returnType, preserveTypeParameters = true)
        val needsReturnCast =
            typeContainsAnyParam(returnType, methodTypeParamNames) && returnType != "Unit"
        val erasedReturnType =
            if (needsReturnCast) eraseTypeParamsToAny(returnType, methodTypeParamNames) else null

        // Build the closure string
        val paramsStr =
            if (closureParams.isEmpty()) "" else "${closureParams.joinToString(", ")} -> "
        val argsStr = behaviorArgs.joinToString(", ")

        val invocation = "behavior($argsStr)"

        val returnExpr =
            if (erasedReturnType != null) {
                "$invocation as $erasedReturnType"
            } else {
                invocation
            }

        return "{ $paramsStr$returnExpr }"
    }

    /** Emits the `@PublishedApi internal fun build()` function into the class. */
    private fun ClassBuilder.emitBuildFunction(
        fakeClassName: String,
        typeArgs: String,
        args: List<String>,
    ) {
        function("build") {
            annotation("PublishedApi")
            internal()
            returns("$fakeClassName$typeArgs")

            if (args.isEmpty()) {
                expressionBody = "$fakeClassName$typeArgs()"
            } else {
                // Indentation: build() lives at class body indent (level 1 = 4 spaces).
                // appendLine only indents the first line; embedded \n need manual indent.
                // Args get 8 spaces (class body + 1 level), closing ) gets 4 spaces (class body).
                val argsStr = args.joinToString(",\n        ")
                expressionBody = "$fakeClassName$typeArgs(\n        $argsStr,\n    )"
            }
        }
    }
}
