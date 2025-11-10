// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.CodeFileBuilder
import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.builder.parseType
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.renderer.render
import com.rsicarelli.fakt.codegen.strategy.DefaultValueResolver

/**
 * Method metadata for fake generation.
 *
 * @property name Method name
 * @property params Parameter list as (name, type, isVararg) triples
 * @property returnType Return type
 * @property isSuspend Whether method is suspend
 * @property isVararg Whether method has vararg parameter (deprecated, use params[].isVararg)
 * @property typeParameters Method-level type parameters (e.g., ["T", "R : Comparable<R>"])
 */
data class MethodSpec(
    val name: String,
    val params: List<Triple<String, String, Boolean>>, // (name, type, isVararg)
    val returnType: String,
    val isSuspend: Boolean = false,
    val isVararg: Boolean = false,  // Kept for backward compatibility, use params[].third
    val typeParameters: List<String> = emptyList(),
)

/**
 * Property metadata for fake generation.
 *
 * @property name Property name
 * @property type Property type
 * @property isStateFlow Whether property is StateFlow
 * @property isMutable Whether property is mutable (var vs val)
 */
data class PropertySpec(
    val name: String,
    val type: String,
    val isStateFlow: Boolean = false,
    val isMutable: Boolean = false,
)

/**
 * Erases method-level type parameters to Any? in a type string.
 *
 * Method-level type parameters (like `<T>`, `<R>`) cannot be used in behavior properties
 * because properties are class-scoped. This function replaces them with `Any?` to match
 * JVM type erasure behavior.
 *
 * @param typeParameters List of type parameter declarations (e.g., ["T", "R : Comparable<R>"])
 * @return Type string with method-level parameters erased to Any?
 */
private fun String.eraseMethodTypeParameters(typeParameters: List<String>): String {
    if (typeParameters.isEmpty()) return this

    // Extract just the type parameter names (remove constraints)
    val typeParamNames = typeParameters.map { param ->
        param.substringBefore(" :").trim()
    }.toSet()

    var result = this
    typeParamNames.forEach { paramName ->
        // Replace type parameter with Any?, but not if it's part of a larger identifier
        // Use word boundary to avoid replacing "T" in "String" or "Test"
        // We DO want to replace "R" in "List<R>" -> "List<Any?>"
        result = result.replace(Regex("\\b$paramName\\b"), "Any?")
    }

    return result
}

/**
 * Generates a complete fake implementation class.
 *
 * Creates a fake with:
 * - Implementation class
 * - Behavior properties for all methods
 * - Override methods delegating to behaviors
 * - StateFlow properties with backing MutableStateFlow
 * - Configuration methods for behavior setup
 *
 * Example:
 * ```kotlin
 * val fake = generateCompleteFake(
 *     packageName = "com.example",
 *     interfaceName = "UserService",
 *     methods = listOf(
 *         MethodSpec("getUser", listOf("id" to "String"), "User?"),
 *         MethodSpec("saveUser", listOf("user" to "User"), "Result<Unit>", isSuspend = true)
 *     ),
 *     properties = listOf(
 *         PropertySpec("users", "List<User>", isStateFlow = true)
 *     ),
 *     typeParameters = listOf("out T : Any")
 * )
 * ```
 *
 * @param packageName Package for generated code
 * @param interfaceName Interface being faked
 * @param methods List of methods to implement
 * @param properties List of properties to implement
 * @param imports Additional imports needed
 * @param header Optional file header comment
 * @param typeParameters Generic type parameters (e.g., ["T", "out T : Any"])
 * @param isClass Whether extending a class (true) vs implementing interface (false)
 * @return CodeFile with complete fake implementation
 */
fun generateCompleteFake(
    packageName: String,
    interfaceName: String,
    methods: List<MethodSpec> = emptyList(),
    properties: List<PropertySpec> = emptyList(),
    imports: List<String> = emptyList(),
    header: String? = null,
    typeParameters: List<String> = emptyList(),
    isClass: Boolean = false,
): CodeFile {
    val className = "Fake${interfaceName}Impl"

    // Extract type parameter names for interface type arguments
    val typeParamNames = typeParameters.map { param ->
        // Extract name from "T", "out T", "T : Bound", "out T : Bound", etc.
        param.trim()
            .removePrefix("out").removePrefix("in")
            .trim()
            .substringBefore(" :")
            .substringBefore(",")
            .trim()
    }

    // Create resolver with class-level type parameters for Array<T> handling
    val resolver = DefaultValueResolver(classLevelTypeParams = typeParamNames.toSet())

    return codeFile(packageName) {
        header?.let { this.header = it }

        // Add common imports
        if (properties.any { it.isStateFlow }) {
            import("kotlinx.coroutines.flow.StateFlow")
            import("kotlinx.coroutines.flow.MutableStateFlow")
        }

        // Add call tracking imports (always needed for call count StateFlows)
        import("kotlinx.coroutines.flow.StateFlow")
        import("kotlinx.coroutines.flow.MutableStateFlow")
        import("kotlinx.coroutines.flow.update")

        // Add custom imports
        imports.forEach { import(it) }

        klass(className) {
            // Parse type parameters and build where clause for multiple constraints
            val whereClauses = mutableListOf<String>()

            typeParameters.forEach { typeParam ->
                // Parse "T : Bound1, Bound2" or "out T : Bound"
                val parts = typeParam.trim().split(" : ", limit = 2)
                val nameWithVariance = parts[0].trim()
                val constraintsStr = if (parts.size > 1) parts[1].trim() else null

                // Extract name without variance
                val name = nameWithVariance
                    .removePrefix("out").removePrefix("in")
                    .trim()

                if (constraintsStr != null) {
                    // Check if there are multiple constraints (comma-separated)
                    val constraints = constraintsStr.split(",").map { it.trim() }

                    if (constraints.size == 1) {
                        // Single constraint: add to type parameter
                        typeParam(name, constraints[0])
                    } else {
                        // Multiple constraints: add to where clause
                        typeParam(name)  // Just the name, no constraints
                        constraints.forEach { constraint ->
                            whereClauses.add("$name : $constraint")
                        }
                    }
                } else {
                    typeParam(name)
                }
            }

            // Add where clause if needed
            if (whereClauses.isNotEmpty()) {
                where(whereClauses.joinToString(", "))
            }

            // Extends class or implements interface with type arguments
            val superType = when {
                typeParamNames.isNotEmpty() -> "$interfaceName<${typeParamNames.joinToString(", ")}>"
                else -> interfaceName
            }
            // Classes need constructor call: ClassName() or ClassName<T>()
            // Interfaces don't: InterfaceName or InterfaceName<T>
            val superTypeWithConstructor = if (isClass) "$superType()" else superType
            implements(superTypeWithConstructor)

            // Generate properties
            properties.forEach { prop ->
                if (prop.isStateFlow) {
                    generateStateFlowProperty(this, prop, resolver)
                } else {
                    generateSimpleProperty(this, prop)
                }
            }

            // Generate methods
            methods.forEach { method ->
                generateMethod(this, method, resolver)
            }
        }
    }
}

/**
 * Extension to generate a complete fake within an existing file.
 */
fun CodeFileBuilder.completeFake(
    interfaceName: String,
    methods: List<MethodSpec> = emptyList(),
    properties: List<PropertySpec> = emptyList(),
) {
    val className = "Fake${interfaceName}Impl"
    val resolver = DefaultValueResolver()

    klass(className) {
        implements(interfaceName)

        // Generate properties
        properties.forEach { prop ->
            if (prop.isStateFlow) {
                generateStateFlowProperty(this, prop, resolver)
            } else {
                generateSimpleProperty(this, prop)
            }
        }

        // Generate methods
        methods.forEach { method ->
            generateMethod(this, method, resolver)
        }
    }
}

/**
 * Generates a StateFlow property with backing MutableStateFlow.
 */
private fun generateStateFlowProperty(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    resolver: DefaultValueResolver,
) {
    // Extract element type from "StateFlow<T>"
    val elementType = prop.type.substringAfter("<").substringBeforeLast(">")

    // Parse element type to get default value
    val parsedType = parseType(elementType)
    val defaultValue = resolver.resolve(parsedType)
    val defaultExpr = defaultValue.render()

    classBuilder.stateFlowProperty(
        name = prop.name,
        elementType = elementType,
        defaultValue = defaultExpr
    )
}

/**
 * Generates a simple property implementation with behavior pattern.
 *
 * For immutable properties (val):
 * ```kotlin
 * private var {name}Behavior: () -> Type = { defaultValue }
 * override val {name}: Type
 *     get() {
 *         _{name}CallCount.update { it + 1 }
 *         return {name}Behavior()
 *     }
 * internal fun configure{Name}(behavior: () -> Type) {
 *     {name}Behavior = behavior
 * }
 * ```
 *
 * For mutable properties (var):
 * ```kotlin
 * private var {name}Getter: () -> Type = { defaultValue }
 * private var {name}Setter: (Type) -> Unit = { }
 * override var {name}: Type
 *     get() {
 *         _{name}CallCount.update { it + 1 }
 *         return {name}Getter()
 *     }
 *     set(value) {
 *         _set{Name}CallCount.update { it + 1 }
 *         {name}Setter(value)
 *     }
 * internal fun configure{Name}(behavior: () -> Type) {
 *     {name}Getter = behavior
 * }
 * internal fun configureSet{Name}(behavior: (Type) -> Unit) {
 *     {name}Setter = behavior
 * }
 * ```
 */
private fun generateSimpleProperty(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
) {
    val parsedType = parseType(prop.type)
    val resolver = DefaultValueResolver()
    val defaultValue = resolver.resolve(parsedType)
    val defaultExpr = defaultValue.render()
    val capitalizedName = prop.name.replaceFirstChar { it.uppercase() }

    // Generate call tracking for property getter
    classBuilder.propertyGetterTracking(prop.name)

    // Generate call tracking for property setter (if mutable)
    if (prop.isMutable) {
        classBuilder.propertySetterTracking(prop.name)
    }

    if (prop.isMutable) {
        // Mutable property: separate getter and setter behaviors

        // Getter behavior property
        classBuilder.property("${prop.name}Getter", "() -> ${prop.type}") {
            private()
            mutable()
            initializer = "{ $defaultExpr }"
        }

        // Setter behavior property
        classBuilder.property("${prop.name}Setter", "(${prop.type}) -> Unit") {
            private()
            mutable()
            initializer = "{ }"
        }

        // Override property with getter/setter using behaviors
        classBuilder.property(prop.name, prop.type) {
            override()
            mutable()
            // Getter with call tracking and behavior invocation
            // Split into separate statements for proper rendering
            getter = listOf(
                "_${prop.name}CallCount.update { it + 1 }",
                "return ${prop.name}Getter()"
            ).joinToString("\n")
            // Setter with call tracking and behavior invocation
            setter = listOf(
                "_set${capitalizedName}CallCount.update { it + 1 }",
                "${prop.name}Setter(value)"
            ).joinToString("\n")
        }

        // Configure method for getter
        classBuilder.function("configure$capitalizedName") {
            internal()
            parameter("behavior", "() -> ${prop.type}")
            returns("Unit")
            body = "${prop.name}Getter = behavior"
        }

        // Configure method for setter
        classBuilder.function("configureSet$capitalizedName") {
            internal()
            parameter("behavior", "(${prop.type}) -> Unit")
            returns("Unit")
            body = "${prop.name}Setter = behavior"
        }
    } else {
        // Immutable property: single behavior for getter

        // Behavior property
        classBuilder.property("${prop.name}Behavior", "() -> ${prop.type}") {
            private()
            mutable()
            initializer = "{ $defaultExpr }"
        }

        // Override property with getter using behavior
        classBuilder.property(prop.name, prop.type) {
            override()
            // Getter with call tracking and behavior invocation
            getter = listOf(
                "_${prop.name}CallCount.update { it + 1 }",
                "return ${prop.name}Behavior()"
            ).joinToString("\n")
        }

        // Configure method
        classBuilder.function("configure$capitalizedName") {
            internal()
            parameter("behavior", "() -> ${prop.type}")
            returns("Unit")
            body = "${prop.name}Behavior = behavior"
        }
    }
}

/**
 * Generates a method implementation with behavior property.
 */
private fun generateMethod(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    resolver: DefaultValueResolver,
) {
    // Get default value for return type
    val parsedReturnType = parseType(method.returnType)
    val defaultValue = resolver.resolve(parsedReturnType)
    val defaultExpr = defaultValue.render()

    // Generate lambda with correct arity
    val paramNames = method.params.mapIndexed { i, _ -> "p$i" }
    val lambdaParams = if (paramNames.isEmpty()) "" else "${paramNames.joinToString(", ")} -> "
    val behaviorDefault = "{ $lambdaParams$defaultExpr }"

    // For varargs, add 'out' variance to Array types for behavior properties
    // vararg messages: String -> behavior: (Array<out String>) -> ReturnType
    val behaviorParamTypes = method.params.map { (_, paramType, isVararg) ->
        if (isVararg && paramType.startsWith("Array<")) {
            // Replace Array<T> with Array<out T>
            paramType.replace("Array<", "Array<out ")
        } else {
            paramType
        }
    }

    // Erase method-level type parameters to Any? for behavior properties
    // Properties cannot have type parameters, so we use type erasure
    val erasedParamTypes = behaviorParamTypes.map { it.eraseMethodTypeParameters(method.typeParameters) }
    val erasedReturnType = method.returnType.eraseMethodTypeParameters(method.typeParameters)

    // Generate call tracking StateFlow property
    classBuilder.callTrackingProperty(method.name)

    // Generate behavior property with erased types
    if (method.isSuspend) {
        classBuilder.suspendBehaviorProperty(
            methodName = method.name,
            paramTypes = erasedParamTypes,
            returnType = erasedReturnType,
            defaultValue = behaviorDefault
        )
    } else {
        classBuilder.behaviorProperty(
            methodName = method.name,
            paramTypes = erasedParamTypes,
            returnType = erasedReturnType,
            defaultValue = behaviorDefault
        )
    }

    // Generate override method
    if (method.isVararg && method.params.size == 1) {
        val (varargName, varargType, _) = method.params.first()
        classBuilder.overrideVarargMethod(
            name = method.name,
            varargName = varargName,
            varargType = varargType,
            returnType = method.returnType
        )
    } else {
        classBuilder.overrideMethod(
            name = method.name,
            params = method.params,
            returnType = method.returnType,
            isSuspend = method.isSuspend,
            typeParameters = method.typeParameters
        )
    }

    // Generate configuration method (use same param types as behavior property)
    classBuilder.configureMethod(
        methodName = method.name,
        paramTypes = behaviorParamTypes,
        returnType = method.returnType,
        isSuspend = method.isSuspend,
        typeParameters = method.typeParameters
    )
}
