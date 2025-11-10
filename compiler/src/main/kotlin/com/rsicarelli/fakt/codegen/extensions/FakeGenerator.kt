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
 * @property isAbstract Whether method is abstract (true) or open (false) - only meaningful for classes
 * @property isOperator Whether method is declared with 'operator' modifier
 * @property extensionReceiverType Extension receiver type for extension functions (e.g., "Vector")
 */
data class MethodSpec(
    val name: String,
    val params: List<Triple<String, String, Boolean>>, // (name, type, isVararg)
    val returnType: String,
    val isSuspend: Boolean = false,
    val isVararg: Boolean = false,  // Kept for backward compatibility, use params[].third
    val typeParameters: List<String> = emptyList(),
    val isAbstract: Boolean = false,  // true for abstract methods, false for open methods
    val isOperator: Boolean = false,  // true for operator functions (plus, get, etc.)
    val extensionReceiverType: String? = null,  // Extension receiver type for extension functions
)

/**
 * Property metadata for fake generation.
 *
 * @property name Property name
 * @property type Property type
 * @property isStateFlow Whether property is StateFlow
 * @property isMutable Whether property is mutable (var vs val)
 * @property isAbstract Whether property is abstract (true) or open (false) - only meaningful for classes
 */
data class PropertySpec(
    val name: String,
    val type: String,
    val isStateFlow: Boolean = false,
    val isMutable: Boolean = false,
    val isAbstract: Boolean = false,  // true for abstract properties, false for open properties
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

            // ==========================================
            // SECTION 1: Call Count StateFlows
            // ==========================================
            // Filter out StateFlow properties (they have their own tracking)
            val simpleProperties = properties.filter { !it.isStateFlow }

            // Generate call tracking for all properties
            simpleProperties.forEach { prop ->
                generatePropertyCallTracking(this, prop)
            }

            // Generate call tracking for all methods
            methods.forEach { method ->
                generateMethodCallTracking(this, method)
            }

            // ==========================================
            // SECTION 2: Behavior Properties
            // ==========================================
            // Generate behavior properties for all simple properties
            simpleProperties.forEach { prop ->
                generatePropertyBehaviorProperty(this, prop, isClass)
            }

            // Generate behavior properties for all methods
            methods.forEach { method ->
                generateMethodBehaviorProperty(this, method, resolver, isClass, interfaceName)
            }

            // ==========================================
            // SECTION 3: Override Implementations
            // ==========================================
            // Generate StateFlow property overrides (these handle tracking internally)
            properties.filter { it.isStateFlow }.forEach { prop ->
                generateStateFlowProperty(this, prop, resolver)
            }

            // Generate property overrides for simple properties
            simpleProperties.forEach { prop ->
                generatePropertyOverride(this, prop, isClass)
            }

            // Generate method overrides
            methods.forEach { method ->
                generateMethodOverride(this, method, isClass)
            }

            // ==========================================
            // SECTION 4: Internal Configuration Methods
            // ==========================================
            // Generate configuration methods for simple properties
            simpleProperties.forEach { prop ->
                generatePropertyConfigMethod(this, prop)
            }

            // Generate configuration methods for all methods
            methods.forEach { method ->
                generateMethodConfigMethod(this, method)
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
 * For immutable properties (val) in interfaces/abstract:
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
 * For immutable properties (val) in open classes:
 * ```kotlin
 * private var {name}Behavior: (() -> Type)? = null
 * override val {name}: Type
 *     get() {
 *         _{name}CallCount.update { it + 1 }
 *         return {name}Behavior?.invoke() ?: super.{name}
 *     }
 * internal fun configure{Name}(behavior: () -> Type) {
 *     {name}Behavior = behavior
 * }
 * ```
 *
 * For mutable properties (var) - similar patterns apply:
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
    isClass: Boolean = false,
) {
    // Determine if this is an open property that should delegate to super
    val isOpenProperty = isClass && !prop.isAbstract

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

        if (isOpenProperty) {
            // Open property: nullable behaviors with super delegation

            // Getter behavior property (nullable)
            classBuilder.property("${prop.name}Getter", "(() -> ${prop.type})?") {
                private()
                mutable()
                initializer = "null"
            }

            // Setter behavior property (nullable)
            classBuilder.property("${prop.name}Setter", "((${prop.type}) -> Unit)?") {
                private()
                mutable()
                initializer = "null"
            }
        } else {
            // Abstract/interface property: non-nullable with defaults

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
        }

        // Override property with getter/setter using behaviors
        classBuilder.property(prop.name, prop.type) {
            override()
            mutable()
            // Getter with call tracking and behavior invocation
            getter = if (isOpenProperty) {
                // Open property: nullable invoke with super delegation
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Getter?.invoke() ?: super.${prop.name}"
                ).joinToString("\n")
            } else {
                // Abstract/interface: direct call
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Getter()"
                ).joinToString("\n")
            }
            // Setter with call tracking and behavior invocation
            setter = if (isOpenProperty) {
                // Open property: nullable invoke with super delegation
                listOf(
                    "_set${capitalizedName}CallCount.update { it + 1 }",
                    "${prop.name}Setter?.invoke(value) ?: run { super.${prop.name} = value }"
                ).joinToString("\n")
            } else {
                // Abstract/interface: direct call
                listOf(
                    "_set${capitalizedName}CallCount.update { it + 1 }",
                    "${prop.name}Setter(value)"
                ).joinToString("\n")
            }
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

        if (isOpenProperty) {
            // Open property: nullable behavior with super delegation
            classBuilder.property("${prop.name}Behavior", "(() -> ${prop.type})?") {
                private()
                mutable()
                initializer = "null"
            }
        } else {
            // Abstract/interface property: non-nullable with default
            classBuilder.property("${prop.name}Behavior", "() -> ${prop.type}") {
                private()
                mutable()
                initializer = "{ $defaultExpr }"
            }
        }

        // Override property with getter using behavior
        classBuilder.property(prop.name, prop.type) {
            override()
            // Getter with call tracking and behavior invocation
            getter = if (isOpenProperty) {
                // Open property: nullable invoke with super delegation
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Behavior?.invoke() ?: super.${prop.name}"
                ).joinToString("\n")
            } else {
                // Abstract/interface: direct call
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Behavior()"
                ).joinToString("\n")
            }
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
 * Detects if a method should use identity function as default.
 *
 * Universal Identity Pattern: `(T) -> T` (input type == output type)
 * - Method has exactly ONE parameter
 * - Parameter type matches return type (ignoring nullability)
 * - Default should be identity: `{ it }`
 *
 * Examples (MATCHES):
 * - `fun transform(input: T): T` → `{ it }`
 * - `fun save(user: User): User` → `{ it }`
 * - `fun handle(input: String?): String?` → `{ it }`
 * - `suspend fun process(items: List<String>): List<String>` → `{ it }`
 *
 * Examples (DOES NOT MATCH):
 * - `fun combine(a: T, b: T): T` → null (multiple parameters)
 * - `fun convert(input: String): Int` → null (different types)
 *
 * @return Identity lambda `{ it }` if pattern matches, null otherwise
 */
private fun shouldUseIdentityDefault(method: MethodSpec): String? {
    // Extension functions have an implicit receiver parameter, so identity doesn't work
    // The behavior lambda needs TWO parameters: receiver + regular parameter
    if (method.extensionReceiverType != null) return null

    // Must have exactly ONE parameter
    if (method.params.size != 1) return null

    val (_, paramType, _) = method.params.first()
    val returnType = method.returnType

    // Compare types ignoring nullability
    val paramBase = paramType.replace("?", "").trim()
    val returnBase = returnType.replace("?", "").trim()

    // If param type == return type, use identity
    return if (paramBase == returnBase) "{ it }" else null
}

/**
 * Detects if a method matches the function invocation pattern for generics.
 *
 * Strict Pattern: `fun <T> method(func: () -> T): T`
 * - Method has exactly ONE parameter
 * - That parameter is a function that takes NO arguments: `() -> T` or `suspend () -> T`
 * - Method returns the same generic type `T`
 * - Default should invoke that function parameter
 *
 * Examples (MATCHES):
 * - `fun <T> executeStep(step: () -> T): T` → `{ step -> step() }`
 * - `suspend fun <T> executeAsyncStep(step: suspend () -> T): T` → `{ step -> step() }`
 *
 * Examples (DOES NOT MATCH):
 * - `fun <T> process(input: T): T` → null (no function parameter)
 * - `fun <R> transform(items: List<T>, mapper: (T) -> R): List<R>` → null (mapper takes argument)
 * - `fun <T> combine(a: T, b: T): T` → null (multiple parameters)
 *
 * @return Identity lambda string if pattern matches, null otherwise
 */
private fun detectIdentityFunction(method: MethodSpec): String? {
    // Must have at least one type parameter
    if (method.typeParameters.isEmpty()) return null

    // Must have exactly ONE parameter (strict requirement)
    if (method.params.size != 1) return null

    // Extract type parameter names (e.g., "T", "R")
    val typeParamNames = method.typeParameters.map { param ->
        param.substringBefore(" :").trim()
    }.toSet()

    // Method must return a type parameter
    if (!typeParamNames.any { method.returnType.contains(Regex("\\b$it\\b")) }) {
        return null
    }

    val (_, paramType, _) = method.params.first()

    // Parameter must be a function type
    if (!paramType.contains("->")) return null

    // Function must take NO arguments: () -> T or suspend () -> T
    // Extract function signature (remove suspend if present)
    val funcSignature = paramType.replace("suspend ", "").trim()

    // Check if it matches () -> T pattern (no arguments before ->)
    val beforeArrow = funcSignature.substringBefore("->").trim()
    if (beforeArrow != "()") return null

    // Check if return type contains a type parameter
    val returnPart = funcSignature.substringAfter("->").trim()
    val returnsTypeParam = typeParamNames.any { typeParam ->
        returnPart.contains(Regex("\\b$typeParam\\b"))
    }
    if (!returnsTypeParam) return null

    // CRITICAL: Method return type must EXACTLY match function return type
    // Good: fun <T> execute(step: () -> T): T  (both return T)
    // Bad:  fun <T> tryOp(op: () -> T): Result<T>  (T vs Result<T>)
    if (method.returnType.trim() != returnPart.trim()) return null

    // Generate identity lambda: { p0 -> p0() }
    return "{ p0 -> p0() }"
}

/**
 * Generates a method implementation with behavior property.
 *
 * For open class methods (isClass=true, isAbstract=false), generates nullable behavior
 * with super delegation. For abstract methods and interfaces, generates non-nullable
 * behavior with error() default.
 */
private fun generateMethod(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    resolver: DefaultValueResolver,
    isClass: Boolean = false,
    className: String = "",
) {
    // Determine if this is an open method that should delegate to super
    val isOpenMethod = isClass && !method.isAbstract

    // Get default value for return type
    val parsedReturnType = parseType(method.returnType)
    val defaultValue = resolver.resolve(parsedReturnType)
    val defaultExpr = defaultValue.render()

    // Generate lambda with correct arity
    // For extension functions, prepend receiver parameter name
    val baseParamNames = method.params.mapIndexed { i, _ -> "p$i" }
    val paramNames = if (method.extensionReceiverType != null) {
        listOf("p_receiver") + baseParamNames
    } else {
        baseParamNames
    }
    val lambdaParams = if (paramNames.isEmpty()) "" else "${paramNames.joinToString(", ")} -> "

    // Priority order for default values:
    // 1. Abstract method errors (MUST fail when not configured)
    // 2. Function invocation: <T> method(func: () -> T): T pattern
    // 3. Identity: (T) -> T pattern (same input/output type)
    // 4. Typed defaults (null, emptyList(), etc.)

    // Generate default behavior with correct priority
    val behaviorDefault = when {
        isClass && method.isAbstract -> {
            // Abstract method in class: ALWAYS throw error (highest priority)
            val methodSignature = "${method.name}(${method.params.joinToString { it.second }}): ${method.returnType}"
            val errorMessage = "Abstract method '$methodSignature' in class '$className' must be configured. " +
                    "Use the DSL: fake${className.replaceFirstChar { it.uppercase() }} { ${method.name} { ... } }"
            "{ ${lambdaParams}error(\"$errorMessage\") }"
        }
        else -> {
            // For interfaces, check special patterns first
            val functionInvocationDefault = detectIdentityFunction(method)
            val identityDefault = if (functionInvocationDefault == null) {
                shouldUseIdentityDefault(method)
            } else null

            when {
                functionInvocationDefault != null -> {
                    // Function invocation: <T> method(func: () -> T): T → { func -> func() }
                    functionInvocationDefault
                }
                identityDefault != null -> {
                    // Universal identity: (T) -> T → { it }
                    identityDefault
                }
                else -> {
                    // Interface method or default: use typed default
                    "{ $lambdaParams$defaultExpr }"
                }
            }
        }
    }

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

    // For extension functions, prepend receiver type to behavior parameter list
    // Extension: fun Vector.plus(other: Vector) -> behavior: (Vector, Vector) -> Vector
    val behaviorFinalParamTypes = if (method.extensionReceiverType != null) {
        val erasedReceiverType = method.extensionReceiverType.eraseMethodTypeParameters(method.typeParameters)
        listOf(erasedReceiverType) + erasedParamTypes
    } else {
        erasedParamTypes
    }

    // Configuration method uses non-erased types with receiver prepended
    val configureFinalParamTypes = if (method.extensionReceiverType != null) {
        listOf(method.extensionReceiverType) + behaviorParamTypes
    } else {
        behaviorParamTypes
    }

    // Generate call tracking StateFlow property
    classBuilder.callTrackingProperty(method.name)

    // Generate behavior property with erased types
    // For open methods: nullable property with null default
    // For abstract/interface methods: non-nullable property with error default
    if (isOpenMethod) {
        // Open method: nullable behavior, will delegate to super when null
        if (method.isSuspend) {
            classBuilder.nullableSuspendBehaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType
            )
        } else {
            classBuilder.nullableBehaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType
            )
        }
    } else {
        // Abstract or interface method: non-nullable with default
        if (method.isSuspend) {
            classBuilder.suspendBehaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType,
                defaultValue = behaviorDefault
            )
        } else {
            classBuilder.behaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType,
                defaultValue = behaviorDefault
            )
        }
    }

    // Generate override method
    // For open methods, pass super delegation flag
    if (method.isVararg && method.params.size == 1) {
        val (varargName, varargType, _) = method.params.first()
        classBuilder.overrideVarargMethod(
            name = method.name,
            varargName = varargName,
            varargType = varargType,
            returnType = method.returnType,
            useSuperDelegation = isOpenMethod,
            extensionReceiverType = method.extensionReceiverType,
            isOperator = method.isOperator
        )
    } else {
        classBuilder.overrideMethod(
            name = method.name,
            params = method.params,
            returnType = method.returnType,
            isSuspend = method.isSuspend,
            typeParameters = method.typeParameters,
            useSuperDelegation = isOpenMethod,
            extensionReceiverType = method.extensionReceiverType,
            isOperator = method.isOperator
        )
    }

    // Generate configuration method (use same param types as behavior property)
    classBuilder.configureMethod(
        methodName = method.name,
        paramTypes = configureFinalParamTypes,
        returnType = method.returnType,
        isSuspend = method.isSuspend,
        typeParameters = method.typeParameters
    )
}

// ==========================================
// SECTION-BASED GENERATION FUNCTIONS
// ==========================================
// These functions support reorganized code generation where declarations
// are grouped by type (call tracking, behaviors, overrides, configs)
// rather than interleaved per-member.

/**
 * Generates ONLY call tracking StateFlows for a method.
 * Part of Section 1: Call Count StateFlows
 */
private fun generateMethodCallTracking(
    classBuilder: ClassBuilder,
    method: MethodSpec,
) {
    classBuilder.callTrackingProperty(method.name)
}

/**
 * Generates ONLY call tracking StateFlows for a property.
 * Part of Section 1: Call Count StateFlows
 */
private fun generatePropertyCallTracking(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
) {
    classBuilder.propertyGetterTracking(prop.name)
    if (prop.isMutable) {
        classBuilder.propertySetterTracking(prop.name)
    }
}

/**
 * Generates ONLY the behavior property for a method.
 * Part of Section 2: Behavior Properties
 */
private fun generateMethodBehaviorProperty(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    resolver: DefaultValueResolver,
    isClass: Boolean = false,
    className: String = "",
) {
    val isOpenMethod = isClass && !method.isAbstract

    val parsedReturnType = parseType(method.returnType)
    val defaultValue = resolver.resolve(parsedReturnType)
    val defaultExpr = defaultValue.render()

    val baseParamNames = method.params.mapIndexed { i, _ -> "p$i" }
    val paramNames = if (method.extensionReceiverType != null) {
        listOf("p_receiver") + baseParamNames
    } else {
        baseParamNames
    }
    val lambdaParams = if (paramNames.isEmpty()) "" else "${paramNames.joinToString(", ")} -> "

    val behaviorDefault = when {
        isClass && method.isAbstract -> {
            val methodSignature = "${method.name}(${method.params.joinToString { it.second }}): ${method.returnType}"
            val errorMessage = "Abstract method '$methodSignature' in class '$className' must be configured. " +
                    "Use the DSL: fake${className.replaceFirstChar { it.uppercase() }} { ${method.name} { ... } }"
            "{ ${lambdaParams}error(\"$errorMessage\") }"
        }
        else -> {
            val functionInvocationDefault = detectIdentityFunction(method)
            val identityDefault = if (functionInvocationDefault == null) {
                shouldUseIdentityDefault(method)
            } else null

            when {
                functionInvocationDefault != null -> functionInvocationDefault
                identityDefault != null -> identityDefault
                else -> "{ $lambdaParams$defaultExpr }"
            }
        }
    }

    val behaviorParamTypes = method.params.map { (_, paramType, isVararg) ->
        if (isVararg && paramType.startsWith("Array<")) {
            paramType.replace("Array<", "Array<out ")
        } else {
            paramType
        }
    }

    val erasedParamTypes = behaviorParamTypes.map { it.eraseMethodTypeParameters(method.typeParameters) }
    val erasedReturnType = method.returnType.eraseMethodTypeParameters(method.typeParameters)

    val behaviorFinalParamTypes = if (method.extensionReceiverType != null) {
        val erasedReceiverType = method.extensionReceiverType.eraseMethodTypeParameters(method.typeParameters)
        listOf(erasedReceiverType) + erasedParamTypes
    } else {
        erasedParamTypes
    }

    if (isOpenMethod) {
        if (method.isSuspend) {
            classBuilder.nullableSuspendBehaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType
            )
        } else {
            classBuilder.nullableBehaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType
            )
        }
    } else {
        if (method.isSuspend) {
            classBuilder.suspendBehaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType,
                defaultValue = behaviorDefault
            )
        } else {
            classBuilder.behaviorProperty(
                methodName = method.name,
                paramTypes = behaviorFinalParamTypes,
                returnType = erasedReturnType,
                defaultValue = behaviorDefault
            )
        }
    }
}

/**
 * Generates ONLY the behavior property for a simple property.
 * Part of Section 2: Behavior Properties
 */
private fun generatePropertyBehaviorProperty(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    isClass: Boolean = false,
) {
    val isOpenProperty = isClass && !prop.isAbstract
    val parsedType = parseType(prop.type)
    val resolver = DefaultValueResolver()
    val defaultValue = resolver.resolve(parsedType)
    val defaultExpr = defaultValue.render()

    if (prop.isMutable) {
        if (isOpenProperty) {
            classBuilder.property("${prop.name}Getter", "(() -> ${prop.type})?") {
                private()
                mutable()
                initializer = "null"
            }
            classBuilder.property("${prop.name}Setter", "((${prop.type}) -> Unit)?") {
                private()
                mutable()
                initializer = "null"
            }
        } else {
            classBuilder.property("${prop.name}Getter", "() -> ${prop.type}") {
                private()
                mutable()
                initializer = "{ $defaultExpr }"
            }
            classBuilder.property("${prop.name}Setter", "(${prop.type}) -> Unit") {
                private()
                mutable()
                initializer = "{ }"
            }
        }
    } else {
        if (isOpenProperty) {
            classBuilder.property("${prop.name}Behavior", "(() -> ${prop.type})?") {
                private()
                mutable()
                initializer = "null"
            }
        } else {
            classBuilder.property("${prop.name}Behavior", "() -> ${prop.type}") {
                private()
                mutable()
                initializer = "{ $defaultExpr }"
            }
        }
    }
}

/**
 * Generates ONLY the override method implementation.
 * Part of Section 3: Override Implementations
 */
private fun generateMethodOverride(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    isClass: Boolean = false,
) {
    val isOpenMethod = isClass && !method.isAbstract

    if (method.isVararg && method.params.size == 1) {
        val (varargName, varargType, _) = method.params.first()
        classBuilder.overrideVarargMethod(
            name = method.name,
            varargName = varargName,
            varargType = varargType,
            returnType = method.returnType,
            useSuperDelegation = isOpenMethod,
            extensionReceiverType = method.extensionReceiverType,
            isOperator = method.isOperator
        )
    } else {
        classBuilder.overrideMethod(
            name = method.name,
            params = method.params,
            returnType = method.returnType,
            isSuspend = method.isSuspend,
            typeParameters = method.typeParameters,
            useSuperDelegation = isOpenMethod,
            extensionReceiverType = method.extensionReceiverType,
            isOperator = method.isOperator
        )
    }
}

/**
 * Generates ONLY the override property implementation.
 * Part of Section 3: Override Implementations
 */
private fun generatePropertyOverride(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    isClass: Boolean = false,
) {
    val isOpenProperty = isClass && !prop.isAbstract
    val capitalizedName = prop.name.replaceFirstChar { it.uppercase() }

    if (prop.isMutable) {
        classBuilder.property(prop.name, prop.type) {
            override()
            mutable()
            getter = if (isOpenProperty) {
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Getter?.invoke() ?: super.${prop.name}"
                ).joinToString("\n")
            } else {
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Getter()"
                ).joinToString("\n")
            }
            setter = if (isOpenProperty) {
                listOf(
                    "_set${capitalizedName}CallCount.update { it + 1 }",
                    "${prop.name}Setter?.invoke(value) ?: run { super.${prop.name} = value }"
                ).joinToString("\n")
            } else {
                listOf(
                    "_set${capitalizedName}CallCount.update { it + 1 }",
                    "${prop.name}Setter(value)"
                ).joinToString("\n")
            }
        }
    } else {
        classBuilder.property(prop.name, prop.type) {
            override()
            getter = if (isOpenProperty) {
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Behavior?.invoke() ?: super.${prop.name}"
                ).joinToString("\n")
            } else {
                listOf(
                    "_${prop.name}CallCount.update { it + 1 }",
                    "return ${prop.name}Behavior()"
                ).joinToString("\n")
            }
        }
    }
}

/**
 * Generates ONLY the configuration method.
 * Part of Section 4: Internal Configuration Methods
 */
private fun generateMethodConfigMethod(
    classBuilder: ClassBuilder,
    method: MethodSpec,
) {
    val behaviorParamTypes = method.params.map { (_, paramType, isVararg) ->
        if (isVararg && paramType.startsWith("Array<")) {
            paramType.replace("Array<", "Array<out ")
        } else {
            paramType
        }
    }

    val configureFinalParamTypes = if (method.extensionReceiverType != null) {
        listOf(method.extensionReceiverType) + behaviorParamTypes
    } else {
        behaviorParamTypes
    }

    classBuilder.configureMethod(
        methodName = method.name,
        paramTypes = configureFinalParamTypes,
        returnType = method.returnType,
        isSuspend = method.isSuspend,
        typeParameters = method.typeParameters
    )
}

/**
 * Generates ONLY the configuration methods for a property.
 * Part of Section 4: Internal Configuration Methods
 */
private fun generatePropertyConfigMethod(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
) {
    val capitalizedName = prop.name.replaceFirstChar { it.uppercase() }

    if (prop.isMutable) {
        classBuilder.function("configure$capitalizedName") {
            internal()
            parameter("behavior", "() -> ${prop.type}")
            returns("Unit")
            body = "${prop.name}Getter = behavior"
        }
        classBuilder.function("configureSet$capitalizedName") {
            internal()
            parameter("behavior", "(${prop.type}) -> Unit")
            returns("Unit")
            body = "${prop.name}Setter = behavior"
        }
    } else {
        classBuilder.function("configure$capitalizedName") {
            internal()
            parameter("behavior", "() -> ${prop.type}")
            returns("Unit")
            body = "${prop.name}Behavior = behavior"
        }
    }
}
