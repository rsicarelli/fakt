// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.generator

import com.rsicarelli.fakt.codegen.analysis.DeclarationAnnotation
import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.codegen.analysis.FunctionSpec
import com.rsicarelli.fakt.codegen.analysis.ParameterSpec
import com.rsicarelli.fakt.codegen.analysis.PropertySpec
import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.builder.parseType
import com.rsicarelli.fakt.codegen.extensions.eraseTypeParamsToAny
import com.rsicarelli.fakt.codegen.extensions.typeContainsAnyParam
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.renderer.render
import com.rsicarelli.fakt.codegen.strategy.DefaultValueResolver
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Generates configuration DSL classes for fake implementations.
 *
 * The config class acts as a standalone builder — it collects behavior lambdas internally during
 * DSL configuration, then the factory function reads these behaviors to construct an immutable
 * fake. The config class does NOT hold a reference to the fake implementation.
 *
 * Consumes the pure [FakeDeclaration] contract; types arrive pre-rendered, so no `TypeResolution`
 * dependency is needed.
 */
public class ConfigurationDslGenerator {
    private companion object {
        /** Length of "Array<" prefix when extracting generic type from Array<T>. */
        private const val ARRAY_PREFIX_LENGTH = 6
    }

    private val defaultValueResolver = DefaultValueResolver()

    /**
     * Generates a configuration DSL CodeFile for the fake interface implementation.
     *
     * The returned CodeFile contains only the class declaration (no package/imports), suitable for
     * being added to an existing file.
     *
     * @param decl Interface declaration with pre-rendered specs
     * @param fakeClassName Name of the fake implementation class
     * @return CodeFile containing the configuration DSL class
     */
    public fun generateConfigurationDslCodeFile(
        decl: FakeDeclaration.Interface,
        fakeClassName: String,
    ): CodeFile {
        val configClassName = "Fake${decl.simpleName}Config"
        val typeArgs = extractTypeParameterNames(decl.typeParameters)
        val (typeParamsForHeader, whereClause) =
            formatTypeParametersWithWhereClause(decl.typeParameters)
        val propagatedAnnotations = extractPropagatedAnnotations(decl.annotations)

        return codeFile("") {
            klass(configClassName) {
                applyVisibility(decl.visibility)
                addTypeParameters(typeParamsForHeader)

                if (whereClause.isNotEmpty()) {
                    where(whereClause)
                }

                propagatedAnnotations.forEach { annotation ->
                    annotation(annotation.simpleName, annotation.renderedArguments)
                }

                val classTypeParamNames = extractClassTypeParamNames(decl.typeParameters)
                val isMutable = decl.generateMutableBehaviors

                decl.functions.forEach { func ->
                    generateFunctionBehaviorProperty(
                        func,
                        withDefault = !isMutable,
                        classTypeParamNames,
                    )
                }
                decl.properties.forEach { prop ->
                    generatePropertyBehaviorProperty(
                        prop,
                        withDefault = !isMutable,
                        classTypeParamNames,
                    )
                }

                decl.functions.forEach { func ->
                    generateFunctionConfigurator(func, decl.visibility)
                }
                decl.properties.forEach { prop ->
                    generatePropertyConfigurator(prop, decl.visibility)
                }

                generateInterfaceBuildMethod(
                    fakeClassName = fakeClassName,
                    typeArgs = typeArgs,
                    functions = decl.functions,
                    properties = decl.properties,
                    generateMutableBehaviors = isMutable,
                    classTypeParamNames = classTypeParamNames,
                )
            }
        }
    }

    /**
     * Generates a configuration DSL CodeFile for the fake class implementation.
     *
     * @param decl Class declaration with pre-rendered specs
     * @param fakeClassName Name of the fake implementation class
     * @return CodeFile containing the configuration DSL class
     */
    public fun generateConfigurationDslCodeFile(
        decl: FakeDeclaration.Class,
        fakeClassName: String,
    ): CodeFile {
        val configClassName = "Fake${decl.simpleName}Config"
        val typeArgs = extractTypeParameterNames(decl.typeParameters)
        val (typeParamsForHeader, whereClause) =
            formatTypeParametersWithWhereClause(decl.typeParameters)
        val propagatedAnnotations = extractPropagatedAnnotations(decl.annotations)

        return codeFile("") {
            klass(configClassName) {
                applyVisibility(decl.visibility)
                addTypeParameters(typeParamsForHeader)

                if (whereClause.isNotEmpty()) {
                    where(whereClause)
                }

                propagatedAnnotations.forEach { annotation ->
                    annotation(annotation.simpleName, annotation.renderedArguments)
                }

                val classTypeParamNames = extractClassTypeParamNames(decl.typeParameters)
                val isMutable = decl.generateMutableBehaviors

                decl.abstractMethods.forEach { func ->
                    generateFunctionBehaviorProperty(
                        func,
                        withDefault = !isMutable,
                        classTypeParamNames,
                        abstractClassName = if (!isMutable) decl.simpleName else null,
                    )
                }
                decl.openMethods.forEach { func ->
                    generateFunctionBehaviorProperty(func, withDefault = false, classTypeParamNames)
                }
                decl.abstractProperties.forEach { prop ->
                    generatePropertyBehaviorProperty(
                        prop,
                        withDefault = !isMutable,
                        classTypeParamNames,
                    )
                }
                decl.openProperties.forEach { prop ->
                    generatePropertyBehaviorProperty(prop, withDefault = false, classTypeParamNames)
                }

                decl.abstractMethods.forEach { func ->
                    generateFunctionConfigurator(func, decl.visibility)
                }
                decl.openMethods.forEach { func ->
                    generateFunctionConfigurator(func, decl.visibility)
                }
                decl.abstractProperties.forEach { prop ->
                    generatePropertyConfigurator(prop, decl.visibility)
                }
                decl.openProperties.forEach { prop ->
                    generatePropertyConfigurator(prop, decl.visibility)
                }

                generateClassBuildMethod(
                    decl = decl,
                    fakeClassName = fakeClassName,
                    typeArgs = typeArgs,
                    generateMutableBehaviors = isMutable,
                    classTypeParamNames = classTypeParamNames,
                )
            }
        }
    }

    private fun extractMethodTypeParamNames(function: FunctionSpec): Set<String> =
        function.typeParameters.map { it.split(" : ", limit = 2)[0].trim() }.toSet()

    private fun extractClassTypeParamNames(typeParameters: List<String>): Set<String> =
        typeParameters.map { it.split(" : ", limit = 2)[0].trim() }.toSet()

    /**
     * Detects single-param identity pattern: param type == return type → `{ it }`.
     *
     * Extension functions are excluded because the behavior lambda requires TWO parameters.
     */
    private fun shouldUseIdentityDefault(function: FunctionSpec): Boolean {
        if (function.extensionReceiverTypeString != null || function.parameters.size != 1)
            return false
        val paramType = function.parameters[0].typeString.replace("?", "").trim()
        val returnType = function.returnTypeString.replace("?", "").trim()
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
    private fun detectFunctionInvocationDefault(function: FunctionSpec): Boolean {
        if (function.typeParameters.isEmpty() || function.parameters.size != 1) return false

        val typeParamNames = extractMethodTypeParamNames(function)
        val returnType = function.returnTypeString
        val paramType = function.parameters[0].typeString
        val funcSignature = paramType.replace("suspend ", "").trim()
        val returnPart = funcSignature.substringAfter("->").trim()

        return typeContainsAnyParam(returnType, typeParamNames) &&
            paramType.contains("->") &&
            funcSignature.substringBefore("->").trim() == "()" &&
            typeContainsAnyParam(returnPart, typeParamNames) &&
            returnType.trim() == returnPart.trim()
    }

    /** Builds a method signature string for error messages (matches FakeImpl format). */
    private fun buildMethodSignature(function: FunctionSpec): String {
        val params = function.parameters.joinToString { it.typeString }
        return "${function.name}($params): ${function.returnTypeString}"
    }

    private fun ClassBuilder.applyVisibility(visibility: FirVisibility) {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            else -> public()
        }
    }

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
        function: FunctionSpec,
        withDefault: Boolean,
        classTypeParamNames: Set<String> = emptySet(),
        abstractClassName: String? = null,
    ) {
        val baseType = buildBaseBehaviorType(function)
        val methodTypeParamNames = extractMethodTypeParamNames(function)

        val propertyType =
            if (methodTypeParamNames.isNotEmpty()) {
                eraseTypeParamsToAny(baseType, methodTypeParamNames)
            } else {
                baseType
            }

        if (withDefault) {
            val effectiveReturnType =
                if (methodTypeParamNames.isNotEmpty()) {
                    eraseTypeParamsToAny(function.returnTypeString, methodTypeParamNames)
                } else {
                    function.returnTypeString
                }
            val defaultExpr = computeDefaultExpr(effectiveReturnType, classTypeParamNames)
            val lambdaParams = buildErasedLambdaParams(function, methodTypeParamNames)

            val behaviorDefault =
                when {
                    abstractClassName != null -> {
                        val sig = buildMethodSignature(function)
                        val escapedClassName = abstractClassName.replaceFirstChar { it.uppercase() }
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
                compact = true
            }
        } else {
            property("${function.name}Behavior", "($propertyType)?") {
                internal()
                mutable()
                initializer = "null"
                compact = true
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
        property: PropertySpec,
        withDefault: Boolean,
        classTypeParamNames: Set<String> = emptySet(),
    ) {
        val propertyType = property.typeString

        if (withDefault) {
            val defaultExpr = computeDefaultExpr(propertyType, classTypeParamNames)
            property("${property.name}Behavior", "(() -> $propertyType)") {
                internal()
                mutable()
                initializer = "{ $defaultExpr }"
                compact = true
            }
        } else {
            property("${property.name}Behavior", "(() -> $propertyType)?") {
                internal()
                mutable()
                initializer = "null"
                compact = true
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
                    compact = true
                }
            } else {
                property(
                    "set${property.name.replaceFirstChar { it.uppercase() }}Behavior",
                    "((${propertyType}) -> Unit)?",
                ) {
                    internal()
                    mutable()
                    initializer = "null"
                    compact = true
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
        function: FunctionSpec,
        visibility: FirVisibility,
    ) {
        val functionName = function.name
        val methodTypeParamNames = extractMethodTypeParamNames(function)

        val paramSignature = "(${buildBaseBehaviorType(function)})"

        function(functionName) {
            compact = true

            when (visibility) {
                FirVisibility.PUBLIC -> public()
                FirVisibility.INTERNAL -> internal()
                else -> public()
            }

            if (methodTypeParamNames.isNotEmpty()) {
                annotation("Suppress", "\"UNCHECKED_CAST\"")
            }

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
                val closureBody = buildWrappingClosure(function, methodTypeParamNames)
                body = "${functionName}Behavior = $closureBody"
            } else {
                body = "${functionName}Behavior = behavior"
            }
        }
    }

    /**
     * Generates a property configurator method.
     *
     * DSL method that stores the behavior lambda in the config's internal property.
     */
    private fun ClassBuilder.generatePropertyConfigurator(
        property: PropertySpec,
        visibility: FirVisibility,
    ) {
        val propertyName = property.name
        val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }
        val propertyType = property.typeString

        function(propertyName) {
            compact = true
            when (visibility) {
                FirVisibility.PUBLIC -> public()
                FirVisibility.INTERNAL -> internal()
                else -> public()
            }

            parameter("behavior", "() -> $propertyType")
            returns("Unit")
            body = "${propertyName}Behavior = behavior"
        }

        if (property.isMutable) {
            function("set$capitalizedName") {
                compact = true
                when (visibility) {
                    FirVisibility.PUBLIC -> public()
                    FirVisibility.INTERNAL -> internal()
                    else -> public()
                }

                parameter("behavior", "($propertyType) -> Unit")
                returns("Unit")
                body = "set${capitalizedName}Behavior = behavior"
            }
        }
    }

    /**
     * Builds the base behavior function type for a function (non-nullable).
     *
     * Returns the function type string without nullable wrapper, e.g. `(String) -> Int`.
     */
    private fun buildBaseBehaviorType(function: FunctionSpec): String {
        val suspendModifier = if (function.isSuspend) "suspend " else ""

        val regularParamTypes =
            function.parameters.joinToString(", ") { param ->
                if (param.isVararg) {
                    val elementType = unwrapVarargsType(param)
                    "Array<out $elementType>"
                } else {
                    param.typeString
                }
            }

        val parameterTypes =
            if (function.extensionReceiverTypeString != null) {
                val receiverTypeStr = function.extensionReceiverTypeString
                if (regularParamTypes.isEmpty()) {
                    receiverTypeStr
                } else {
                    "$receiverTypeStr, $regularParamTypes"
                }
            } else {
                regularParamTypes
            }

        return if (parameterTypes.isEmpty()) {
            "$suspendModifier() -> ${function.returnTypeString}"
        } else {
            "$suspendModifier($parameterTypes) -> ${function.returnTypeString}"
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
        annotations: List<DeclarationAnnotation>
    ): List<DeclarationAnnotation> =
        annotations.filter {
            (it.simpleName == "OptIn" || it.simpleName == "Deprecated") && !it.isOptInMarker
        }

    /** Unwraps varargs `Array<T>` to element type `T`. */
    private fun unwrapVarargsType(param: ParameterSpec): String {
        val arrayType = param.typeString
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

    private fun extractTypeParameterNames(typeParameters: List<String>): String =
        if (typeParameters.isNotEmpty()) {
            "<${typeParameters.joinToString(", ") { it.substringBefore(" :").trim() }}>"
        } else {
            ""
        }

    /**
     * Generates `@PublishedApi internal fun build()` for interface config classes.
     *
     * When immutable: behaviors have non-nullable defaults, passed through directly. When mutable:
     * behaviors are nullable (null = not configured), build() applies `?: default`.
     */
    private fun ClassBuilder.generateInterfaceBuildMethod(
        fakeClassName: String,
        typeArgs: String,
        functions: List<FunctionSpec>,
        properties: List<PropertySpec>,
        generateMutableBehaviors: Boolean = false,
        classTypeParamNames: Set<String> = emptySet(),
    ) {
        val args = mutableListOf<String>()
        properties.forEach { prop ->
            val fallbackDefault =
                if (generateMutableBehaviors) {
                    computePropertyDefaultLambda(prop, classTypeParamNames)
                } else {
                    null
                }
            addPropertyBuildArg(args = args, prop = prop, fallbackDefault = fallbackDefault)
        }
        functions.forEach { func ->
            val fallbackDefault =
                if (generateMutableBehaviors) {
                    computeFunctionDefaultLambda(func, classTypeParamNames)
                } else {
                    null
                }
            addFunctionBuildArg(args = args, func = func, fallbackDefault = fallbackDefault)
        }
        emitBuildFunction(fakeClassName = fakeClassName, typeArgs = typeArgs, args = args)
    }

    /**
     * Generates `@PublishedApi internal fun build()` for class config classes.
     *
     * When immutable: abstract = non-nullable with defaults, open = nullable (super delegation).
     * When mutable: abstract behaviors are nullable, build() applies `?: default`. Open behaviors
     * remain nullable regardless (null = delegate to super).
     */
    private fun ClassBuilder.generateClassBuildMethod(
        decl: FakeDeclaration.Class,
        fakeClassName: String,
        typeArgs: String,
        generateMutableBehaviors: Boolean = false,
        classTypeParamNames: Set<String> = emptySet(),
    ) {
        val args = mutableListOf<String>()
        decl.abstractProperties.forEach { prop ->
            val fallbackDefault =
                if (generateMutableBehaviors) {
                    computePropertyDefaultLambda(prop, classTypeParamNames)
                } else {
                    null
                }
            addPropertyBuildArg(args = args, prop = prop, fallbackDefault = fallbackDefault)
        }
        decl.openProperties.forEach { addPropertyBuildArg(args = args, prop = it) }
        decl.abstractMethods.forEach { func ->
            val fallbackDefault =
                if (generateMutableBehaviors) {
                    computeFunctionDefaultLambda(
                        func,
                        classTypeParamNames,
                        abstractClassName = decl.simpleName,
                    )
                } else {
                    null
                }
            addFunctionBuildArg(args = args, func = func, fallbackDefault = fallbackDefault)
        }
        decl.openMethods.forEach { addFunctionBuildArg(args = args, func = it) }
        emitBuildFunction(fakeClassName = fakeClassName, typeArgs = typeArgs, args = args)
    }

    /**
     * Adds constructor argument(s) for a property behavior to the build args list.
     *
     * @param fallbackDefault When non-null, adds `?: fallbackDefault` for nullable-because-mutable
     *   properties. Used by mutable fakes where Config behaviors are nullable.
     */
    private fun addPropertyBuildArg(
        args: MutableList<String>,
        prop: PropertySpec,
        fallbackDefault: String? = null,
    ) {
        val propType = prop.typeString
        if (propType.contains("StateFlow<")) return

        if (prop.isMutable) {
            val cap = prop.name.replaceFirstChar { it.uppercase() }
            val getterValue =
                if (fallbackDefault != null) {
                    "${prop.name}Behavior ?: $fallbackDefault"
                } else {
                    "${prop.name}Behavior"
                }
            val setterValue =
                if (fallbackDefault != null) {
                    "set${cap}Behavior ?: { }"
                } else {
                    "set${cap}Behavior"
                }
            args.add("${prop.name}Getter = $getterValue")
            args.add("${prop.name}Setter = $setterValue")
        } else {
            val value =
                if (fallbackDefault != null) {
                    "${prop.name}Behavior ?: $fallbackDefault"
                } else {
                    "${prop.name}Behavior"
                }
            args.add("${prop.name}Behavior = $value")
        }
    }

    /**
     * Adds a constructor argument for a function behavior to the build args list.
     *
     * All methods (including those with method-level type params) are now included — generic method
     * config properties use erased types (Any?) with non-null defaults, matching the FakeImpl
     * constructor parameter types.
     *
     * @param fallbackDefault When non-null, adds `?: fallbackDefault` for nullable-because-mutable
     *   behaviors. Used by mutable fakes where Config behaviors are nullable.
     */
    private fun addFunctionBuildArg(
        args: MutableList<String>,
        func: FunctionSpec,
        fallbackDefault: String? = null,
    ) {
        val value =
            if (fallbackDefault != null) {
                "${func.name}Behavior ?: $fallbackDefault"
            } else {
                "${func.name}Behavior"
            }
        args.add("${func.name}Behavior = $value")
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

    /**
     * Computes the default behavior lambda for a function.
     *
     * Used in build() for mutable fakes to provide `?: default` fallback when Config behaviors are
     * nullable. Mirrors the 4-tier default logic from [generateFunctionBehaviorProperty].
     */
    private fun computeFunctionDefaultLambda(
        function: FunctionSpec,
        classTypeParamNames: Set<String>,
        abstractClassName: String? = null,
    ): String {
        val methodTypeParamNames = extractMethodTypeParamNames(function)
        val effectiveReturnType =
            if (methodTypeParamNames.isNotEmpty()) {
                eraseTypeParamsToAny(function.returnTypeString, methodTypeParamNames)
            } else {
                function.returnTypeString
            }
        val defaultExpr = computeDefaultExpr(effectiveReturnType, classTypeParamNames)
        val lambdaParams = buildErasedLambdaParams(function, methodTypeParamNames)

        return when {
            abstractClassName != null -> {
                val sig = buildMethodSignature(function)
                val escapedClassName = abstractClassName.replaceFirstChar { it.uppercase() }
                val errorMsg =
                    "Abstract method '$sig' in class '$abstractClassName' must be configured. " +
                        "Use the DSL: fake$escapedClassName { ${function.name} { ... } }"
                "{ ${lambdaParams}error(\"$errorMsg\") }"
            }
            detectFunctionInvocationDefault(function) -> "{ p0 -> p0() }"
            shouldUseIdentityDefault(function) -> "{ it }"
            else -> "{ $lambdaParams$defaultExpr }"
        }
    }

    /**
     * Computes the default getter lambda for a property.
     *
     * Used in build() for mutable fakes to provide `?: default` fallback.
     */
    private fun computePropertyDefaultLambda(
        property: PropertySpec,
        classTypeParamNames: Set<String>,
    ): String {
        val defaultExpr = computeDefaultExpr(property.typeString, classTypeParamNames)
        return "{ $defaultExpr }"
    }

    /** Builds lambda parameter placeholders for a function's default behavior. */
    private fun buildLambdaParams(func: FunctionSpec): String {
        val baseParamNames = func.parameters.mapIndexed { i, _ -> "p$i" }
        val paramNames =
            if (func.extensionReceiverTypeString != null) {
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
        func: FunctionSpec,
        methodTypeParamNames: Set<String>,
    ): String {
        if (methodTypeParamNames.isEmpty()) return buildLambdaParams(func)

        val baseParamNames = func.parameters.mapIndexed { i, _ -> "_p$i" }
        val paramNames =
            if (func.extensionReceiverTypeString != null) {
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
        function: FunctionSpec,
        methodTypeParamNames: Set<String>,
    ): String {
        val closureParams = mutableListOf<String>()
        val behaviorArgs = mutableListOf<String>()

        if (function.extensionReceiverTypeString != null) {
            val receiverType = function.extensionReceiverTypeString
            closureParams.add("p_receiver")
            if (typeContainsAnyParam(receiverType, methodTypeParamNames)) {
                behaviorArgs.add("p_receiver as $receiverType")
            } else {
                behaviorArgs.add("p_receiver")
            }
        }

        function.parameters.forEachIndexed { i, param ->
            val paramName = "p$i"
            closureParams.add(paramName)

            val paramType =
                if (param.isVararg) {
                    "Array<out ${unwrapVarargsType(param)}>"
                } else {
                    param.typeString
                }

            if (typeContainsAnyParam(paramType, methodTypeParamNames)) {
                behaviorArgs.add("$paramName as $paramType")
            } else {
                behaviorArgs.add(paramName)
            }
        }

        val returnType = function.returnTypeString
        val needsReturnCast =
            typeContainsAnyParam(returnType, methodTypeParamNames) && returnType != "Unit"
        val erasedReturnType =
            if (needsReturnCast) eraseTypeParamsToAny(returnType, methodTypeParamNames) else null

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
                val argsStr = args.joinToString(",\n        ")
                expressionBody = "$fakeClassName$typeArgs(\n        $argsStr,\n    )"
            }
        }
    }
}
