// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.builder.parseType
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.renderer.render
import com.rsicarelli.fakt.codegen.strategy.DefaultValueResolver
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Method metadata for fake generation.
 *
 * @property name Method name
 * @property params Parameter list as (name, type, isVararg) triples
 * @property returnType Return type
 * @property isSuspend Whether method is suspend
 * @property isVararg Whether method has vararg parameter (deprecated, use params[].isVararg)
 * @property typeParameters Method-level type parameters (e.g., ["T", "R : Comparable<R>"])
 * @property isAbstract Whether method is abstract (true) or open (false) - only meaningful for
 *   classes
 * @property isOperator Whether method is declared with 'operator' modifier
 * @property extensionReceiverType Extension receiver type for extension functions (e.g., "Vector")
 */
data class MethodSpec(
    val name: String,
    val params: List<Triple<String, String, Boolean>>, // (name, type, isVararg)
    val returnType: String,
    val isSuspend: Boolean = false,
    val isVararg: Boolean = false, // Kept for backward compatibility, use params[].third
    val typeParameters: List<String> = emptyList(),
    val isAbstract: Boolean = false, // true for abstract methods, false for open methods
    val isOperator: Boolean = false, // true for operator functions (plus, get, etc.)
    val extensionReceiverType: String? = null, // Extension receiver type for extension functions
    val defaultBehavior: String = "", // Default behavior lambda expression (e.g., "{ null }")
)

/**
 * Property metadata for fake generation.
 *
 * @property name Property name
 * @property type Property type
 * @property isStateFlow Whether property is StateFlow
 * @property isMutable Whether property is mutable (var vs val)
 * @property isAbstract Whether property is abstract (true) or open (false) - only meaningful for
 *   classes
 */
data class PropertySpec(
    val name: String,
    val type: String,
    val isStateFlow: Boolean = false,
    val isMutable: Boolean = false,
    val isAbstract: Boolean = false, // true for abstract properties, false for open properties
    val defaultBehavior: String = "", // Default behavior lambda expression (e.g., "{ 0 }")
)

/**
 * Configuration for complete fake generation.
 *
 * Groups related parameters for [generateCompleteFake] to reduce parameter count.
 *
 * @property generateCallHistory When true, generates call tracking code (call count, call history).
 *   When false, generates lightweight fakes without tracking. Default: true.
 */
data class FakeGenerationConfig(
    val packageName: String,
    val interfaceName: String,
    val methods: List<MethodSpec> = emptyList(),
    val properties: List<PropertySpec> = emptyList(),
    val imports: List<String> = emptyList(),
    val header: String? = null,
    val typeParameters: List<String> = emptyList(),
    val isClass: Boolean = false,
    val visibility: FirVisibility = FirVisibility.PUBLIC,
    val annotations: List<AnnotationSpec> = emptyList(),
    val generateCallHistory: Boolean = true,
)

/**
 * Erases method-level type parameters to Any? in a type string.
 *
 * Method-level type parameters (like `<T>`, `<R>`) cannot be used in behavior properties because
 * properties are class-scoped. This function replaces them with `Any?` to match JVM type erasure
 * behavior.
 *
 * @param typeParameters List of type parameter declarations (e.g., ["T", "R : Comparable<R>"])
 * @return Type string with method-level parameters erased to Any?
 */
private fun String.eraseMethodTypeParameters(typeParameters: List<String>): String {
    if (typeParameters.isEmpty()) return this
    val typeParamNames = typeParameters.map { it.substringBefore(" :").trim() }.toSet()
    return eraseTypeParamsSimple(this, typeParamNames)
}

/**
 * Annotation metadata for fake generation.
 *
 * @property simpleName Simple annotation name (e.g., "OptIn", "Deprecated")
 * @property fullyQualifiedName Fully qualified name for imports (e.g., "kotlin.OptIn")
 * @property arguments Pre-rendered argument strings (e.g., ["ExperimentalApi::class"])
 * @property isOptInMarker True if this annotation is marked with @RequiresOptIn. When true, the
 *   generated fake needs @OptIn(ThisAnnotation::class) to compile.
 */
data class AnnotationSpec(
    val simpleName: String,
    val fullyQualifiedName: String,
    val arguments: List<String> = emptyList(),
    val isOptInMarker: Boolean = false,
)

/**
 * Annotations that require @OptIn to use them. Maps annotation FQN -> required opt-in annotation
 * FQN.
 */
private val ANNOTATIONS_REQUIRING_OPTIN =
    mapOf(
        "kotlin.native.HiddenFromObjC" to "kotlin.experimental.ExperimentalObjCRefinement",
        "kotlin.native.ObjCName" to "kotlin.experimental.ExperimentalObjCName",
    )

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
 *     typeParameters = listOf("out T : Any"),
 *     annotations = listOf(
 *         AnnotationSpec("OptIn", "kotlin.OptIn", listOf("ExperimentalApi::class"))
 *     )
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
 * @param visibility Visibility for the generated class (PUBLIC, INTERNAL) for explicitApi() support
 * @param annotations Annotations to propagate to the generated class
 * @param generateCallHistory When true, generates call tracking code. When false, generates
 *   lightweight fakes without call count or call history tracking. Default: true.
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
    visibility: FirVisibility = FirVisibility.PUBLIC,
    annotations: List<AnnotationSpec> = emptyList(),
    generateCallHistory: Boolean = true,
): CodeFile =
    generateCompleteFakeInternal(
        FakeGenerationConfig(
            packageName = packageName,
            interfaceName = interfaceName,
            methods = methods,
            properties = properties,
            imports = imports,
            header = header,
            typeParameters = typeParameters,
            isClass = isClass,
            visibility = visibility,
            annotations = annotations,
            generateCallHistory = generateCallHistory,
        )
    )

private fun generateCompleteFakeInternal(config: FakeGenerationConfig): CodeFile {
    val packageName = config.packageName
    val interfaceName = config.interfaceName
    val methods = config.methods
    val properties = config.properties
    val imports = config.imports
    val header = config.header
    val typeParameters = config.typeParameters
    val isClass = config.isClass
    val visibility = config.visibility
    val annotations = config.annotations
    val generateCallHistory = config.generateCallHistory
    val className = "Fake${interfaceName}Impl"

    // Extract type parameter names for interface type arguments
    val typeParamNames =
        typeParameters.map { param ->
            // Extract name from "T", "out T", "T : Bound", "out T : Bound", etc.
            param
                .trim()
                .removePrefix("out")
                .removePrefix("in")
                .trim()
                .substringBefore(" :")
                .substringBefore(",")
                .trim()
        }

    // Create resolver with class-level type parameters for Array<T> handling
    val resolver = DefaultValueResolver(classLevelTypeParams = typeParamNames.toSet())

    // Check if any annotation uses ::class references (requires KClass import)
    val needsKClassImport =
        annotations.any { spec -> spec.arguments.any { it.contains("::class") } }

    return codeFile(packageName) {
        header?.let { this.header = it }

        // Add common imports
        if (properties.any { it.isStateFlow }) {
            import("kotlinx.coroutines.flow.StateFlow")
            import("kotlinx.coroutines.flow.MutableStateFlow")
        }

        // Add call tracking imports (only needed when call history is enabled)
        if (generateCallHistory) {
            import("kotlinx.coroutines.flow.StateFlow")
            import("kotlinx.coroutines.flow.MutableStateFlow")
            import("kotlinx.coroutines.flow.update")
        }

        // Add KClass import if needed (for annotations with class references)
        if (needsKClassImport) {
            import("kotlin.reflect.KClass")
        }

        // Add custom imports
        imports.forEach { import(it) }

        // Add imports for annotations
        annotations.forEach { annotationSpec ->
            import(annotationSpec.fullyQualifiedName)
            // Also import required opt-in annotations
            ANNOTATIONS_REQUIRING_OPTIN[annotationSpec.fullyQualifiedName]?.let { requiredOptIn ->
                import(requiredOptIn)
            }
        }

        klass(className) {
            // Apply visibility modifier for explicitApi() support
            when (visibility) {
                FirVisibility.PUBLIC -> public()
                FirVisibility.INTERNAL -> internal()
                FirVisibility.PRIVATE,
                FirVisibility.PROTECTED -> {
                    // Private and protected are not supported for top-level classes
                    // Default to public for safety
                    public()
                }
            }

            // Collect all required @OptIn arguments into a single annotation:
            // 1. Opt-in markers: annotations with @RequiresOptIn need @OptIn(MarkerClass::class)
            // 2. Experimental annotations: annotations that require opt-in to use them
            // 3. Existing @OptIn arguments from the source type
            val optInArgs = mutableListOf<String>()

            // Add opt-in for marker annotations (annotations with @RequiresOptIn)
            annotations
                .filter { it.isOptInMarker }
                .forEach { marker -> optInArgs.add("${marker.simpleName}::class") }

            // Add opt-in for annotations that require it (e.g., @HiddenFromObjC)
            annotations.forEach { spec ->
                ANNOTATIONS_REQUIRING_OPTIN[spec.fullyQualifiedName]?.let { requiredOptIn ->
                    val optInName = requiredOptIn.substringAfterLast(".")
                    optInArgs.add("$optInName::class")
                }
            }

            // Collect existing @OptIn arguments from source to merge
            annotations
                .filter { it.simpleName == "OptIn" }
                .forEach { optInAnnotation -> optInArgs.addAll(optInAnnotation.arguments) }

            // Add single merged @OptIn if any opt-ins are needed
            if (optInArgs.isNotEmpty()) {
                annotation("OptIn", *optInArgs.distinct().toTypedArray())
            }

            // Propagate annotations from source type, EXCEPT:
            // - Opt-in markers (we added @OptIn for them)
            // - @OptIn annotations (we merged them above)
            annotations
                .filter { !it.isOptInMarker && it.simpleName != "OptIn" }
                .forEach { annotationSpec ->
                    annotation(annotationSpec.simpleName, *annotationSpec.arguments.toTypedArray())
                }
            // Parse type parameters and build where clause for multiple constraints
            val whereClauses = mutableListOf<String>()

            typeParameters.forEach { typeParam ->
                // Parse "T : Bound1, Bound2" or "out T : Bound"
                val parts = typeParam.trim().split(" : ", limit = 2)
                val nameWithVariance = parts[0].trim()
                val constraintsStr = if (parts.size > 1) parts[1].trim() else null

                // Extract name without variance
                val name = nameWithVariance.removePrefix("out").removePrefix("in").trim()

                if (constraintsStr != null) {
                    // Check if there are multiple constraints (comma-separated)
                    val constraints = constraintsStr.split(",").map { it.trim() }

                    if (constraints.size == 1) {
                        // Single constraint: add to type parameter
                        typeParam(name, constraints[0])
                    } else {
                        // Multiple constraints: add to where clause
                        typeParam(name) // Just the name, no constraints
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
            val superType =
                when {
                    typeParamNames.isNotEmpty() ->
                        "$interfaceName<${typeParamNames.joinToString(", ")}>"
                    else -> interfaceName
                }
            // Classes need constructor call: ClassName() or ClassName<T>()
            // Interfaces don't: InterfaceName or InterfaceName<T>
            val superTypeWithConstructor = if (isClass) "$superType()" else superType
            implements(superTypeWithConstructor)

            // Filter out StateFlow properties (they have their own tracking)
            val simpleProperties = properties.filter { !it.isStateFlow }

            // ==========================================
            // Constructor: Behavior parameters (immutable)
            // Behaviors are constructor params with defaults,
            // making the fake immutable after construction.
            // ==========================================
            simpleProperties.forEach { prop ->
                generatePropertyBehaviorConstructorParam(this, prop, isClass, className)
            }
            methods.forEach { method ->
                generateMethodBehaviorConstructorParam(this, method, resolver, isClass, className)
            }

            // ==========================================
            // SECTION 1: Interface/Class Implementation
            // ==========================================
            region("$interfaceName Implementation")

            // Generate StateFlow property overrides (these handle tracking internally)
            properties
                .filter { it.isStateFlow }
                .forEach { prop -> generateStateFlowProperty(this, prop, resolver) }

            // Generate property overrides for simple properties
            simpleProperties.forEach { prop ->
                generatePropertyOverride(this, prop, isClass, generateCallHistory)
            }

            // Generate method overrides
            val methodContext =
                MethodOverrideContext(
                    isClass = isClass,
                    interfaceName = interfaceName,
                    classTypeParameters = typeParameters,
                    generateCallHistory = generateCallHistory,
                )
            methods.forEach { method -> generateMethodOverride(this, method, methodContext) }

            endRegion()

            // ==========================================
            // SECTION 2: Call Tracking (public getters)
            // Only generated when call history is enabled
            // ==========================================
            if (generateCallHistory) {
                region("Call Tracking")

                // Generate public call tracking getters for all properties
                simpleProperties.forEach { prop ->
                    generatePropertyCallTrackingPublicGetter(this, prop, visibility)
                }

                // Generate public call tracking getters for all methods
                methods.forEach { method ->
                    generateMethodCallTrackingPublicGetter(this, method, visibility)
                }

                endRegion()
            }

            // ==========================================
            // SECTION 3: Private State (call tracking only)
            // Behavior properties are now immutable constructor
            // params — only call tracking state remains here.
            // ==========================================
            if (generateCallHistory) {
                region("Private State")

                // Generate private backing fields for call tracking (properties only)
                simpleProperties.forEach { prop ->
                    generatePropertyCallTrackingBackingField(this, prop)
                }

                // Generate call history backing fields for ALL methods
                methods.forEach { method ->
                    generateMethodCallHistoryBackingField(this, method, interfaceName)
                }

                endRegion()
            }
        }
    }
}

/** Generates a StateFlow property with backing MutableStateFlow. */
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
        defaultValue = defaultExpr,
    )
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
    // Must have exactly ONE parameter (not extension function)
    if (method.extensionReceiverType != null || method.params.size != 1) return null

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
    // Must have at least one type parameter and exactly ONE parameter (strict requirement)
    if (method.typeParameters.isEmpty() || method.params.size != 1) return null

    // Extract type parameter names (e.g., "T", "R")
    val typeParamNames =
        method.typeParameters.map { param -> param.substringBefore(" :").trim() }.toSet()

    val (_, paramType, _) = method.params.first()

    // Validate function invocation pattern: () -> T where method returns T
    // Generate identity lambda if valid: { p0 -> p0() }
    return if (isValidFunctionInvocationPattern(method, paramType, typeParamNames)) {
        "{ p0 -> p0() }"
    } else {
        null
    }
}

/**
 * Validates if method matches function invocation pattern.
 *
 * Pattern: fun <T> method(func: () -> T): T
 * - Parameter is a function type
 * - Function takes NO arguments: () -> T
 * - Method returns same type T
 */
private fun isValidFunctionInvocationPattern(
    method: MethodSpec,
    paramType: String,
    typeParamNames: Set<String>,
): Boolean {
    // Method must return a type parameter
    if (!typeContainsAnyParam(method.returnType, typeParamNames)) return false

    // Parameter must be a function type
    if (!paramType.contains("->")) return false

    // Function must take NO arguments: () -> T or suspend () -> T
    val funcSignature = paramType.replace("suspend ", "").trim()
    val beforeArrow = funcSignature.substringBefore("->").trim()
    if (beforeArrow != "()") return false

    // Check if return type contains a type parameter
    val returnPart = funcSignature.substringAfter("->").trim()
    if (!typeContainsAnyParam(returnPart, typeParamNames)) return false

    // CRITICAL: Method return type must EXACTLY match function return type
    // Good: fun <T> execute(step: () -> T): T  (both return T)
    // Bad:  fun <T> tryOp(op: () -> T): Result<T>  (T vs Result<T>)
    return method.returnType.trim() == returnPart.trim()
}

/** Generates ONLY the public getter for method call tracking. Part of Section 2: Call Tracking */
private fun generateMethodCallTrackingPublicGetter(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    visibility: FirVisibility,
) {
    classBuilder.callTrackingPublicGetter(method.name, visibility)
}

/**
 * Generates the private backing field for call history tracking. Generated for ALL methods:
 * - Methods with params: stores data class instances
 * - 0-param/vararg-only methods: stores Unit Part of Section 4: Private State
 */
private fun generateMethodCallHistoryBackingField(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    interfaceName: String,
) {
    val storageInfo = resolveHistoryStorageType(interfaceName, method.name, method.params)
    classBuilder.callHistoryBackingField(method.name, storageInfo.dataClassName)
}

/** Generates ONLY the public getter for property call tracking. Part of Section 2: Call Tracking */
private fun generatePropertyCallTrackingPublicGetter(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    visibility: FirVisibility,
) {
    classBuilder.propertyGetterTrackingPublicGetter(prop.name, visibility)
    if (prop.isMutable) {
        classBuilder.propertySetterTrackingPublicGetter(prop.name, visibility)
    }
}

/**
 * Generates ONLY the private backing field for property call tracking. Part of Section 4: Private
 * State
 */
private fun generatePropertyCallTrackingBackingField(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
) {
    classBuilder.propertyGetterTrackingBackingField(prop.name)
    if (prop.isMutable) {
        classBuilder.propertySetterTrackingBackingField(prop.name)
    }
}

/**
 * Generates a `private val` constructor property for a method behavior.
 *
 * The behavior lambda is a direct constructor property with a sensible default. No intermediate
 * member properties needed — the override methods reference the constructor `val` directly.
 *
 * Generated pattern:
 * ```kotlin
 * class FakeXxxImpl(
 *     private val findByIdBehavior: (String) -> User? = { null },
 * ) : Xxx {
 *     override fun findById(id: String): User? = findByIdBehavior(id)
 * }
 * ```
 */
private fun generateMethodBehaviorConstructorParam(
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
    val paramNames =
        if (method.extensionReceiverType != null) {
            listOf("p_receiver") + baseParamNames
        } else {
            baseParamNames
        }
    val lambdaParams = if (paramNames.isEmpty()) "" else "${paramNames.joinToString(", ")} -> "

    val behaviorDefault =
        when {
            isClass && method.isAbstract -> {
                val methodSignature = buildString {
                    append(method.name)
                    append("(")
                    append(method.params.joinToString { it.second })
                    append("): ")
                    append(method.returnType)
                }
                val errorMessage =
                    "Abstract method '$methodSignature' in class '$className' must be configured. " +
                        "Use the DSL: fake${className.replaceFirstChar { it.uppercase() }} { ${method.name} { ... } }"
                "{ ${lambdaParams}error(\"$errorMessage\") }"
            }

            else -> {
                val functionInvocationDefault = detectIdentityFunction(method)
                val identityDefault =
                    if (functionInvocationDefault == null) {
                        shouldUseIdentityDefault(method)
                    } else {
                        null
                    }

                when {
                    functionInvocationDefault != null -> functionInvocationDefault
                    identityDefault != null -> identityDefault
                    else -> "{ $lambdaParams$defaultExpr }"
                }
            }
        }

    val behaviorParamTypes =
        method.params.map { (_, paramType, isVararg) ->
            if (isVararg && paramType.startsWith("Array<")) {
                paramType.replace("Array<", "Array<out ")
            } else {
                paramType
            }
        }

    val erasedParamTypes =
        behaviorParamTypes.map { it.eraseMethodTypeParameters(method.typeParameters) }
    val erasedReturnType = method.returnType.eraseMethodTypeParameters(method.typeParameters)

    val behaviorFinalParamTypes =
        if (method.extensionReceiverType != null) {
            val erasedReceiverType =
                method.extensionReceiverType.eraseMethodTypeParameters(method.typeParameters)
            listOf(erasedReceiverType) + erasedParamTypes
        } else {
            erasedParamTypes
        }

    val functionType =
        buildBehaviorFunctionType(
            paramTypes = behaviorFinalParamTypes,
            returnType = erasedReturnType,
            isSuspend = method.isSuspend,
            isNullable = isOpenMethod,
        )

    // Direct private val constructor property — no intermediate member needed
    classBuilder.constructorProperty("${method.name}Behavior", functionType) {
        private()
        this.defaultValue = if (isOpenMethod) "null" else behaviorDefault
    }
}

/**
 * Generates `private val` constructor properties for a property behavior.
 *
 * Direct constructor properties with sensible defaults — no intermediate member properties needed.
 */
private fun generatePropertyBehaviorConstructorParam(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    isClass: Boolean = false,
    @Suppress("UNUSED_PARAMETER") className: String = "",
) {
    val isOpenProperty = isClass && !prop.isAbstract
    val parsedType = parseType(prop.type)
    val resolver = DefaultValueResolver()
    val defaultValue = resolver.resolve(parsedType)
    val defaultExpr = defaultValue.render()

    if (prop.isMutable) {
        if (isOpenProperty) {
            classBuilder.constructorProperty("${prop.name}Getter", "(() -> ${prop.type})?") {
                private()
                this.defaultValue = "null"
            }
            classBuilder.constructorProperty("${prop.name}Setter", "((${prop.type}) -> Unit)?") {
                private()
                this.defaultValue = "null"
            }
        } else {
            classBuilder.constructorProperty("${prop.name}Getter", "() -> ${prop.type}") {
                private()
                this.defaultValue = "{ $defaultExpr }"
            }
            classBuilder.constructorProperty("${prop.name}Setter", "(${prop.type}) -> Unit") {
                private()
                this.defaultValue = "{ }"
            }
        }
    } else {
        if (isOpenProperty) {
            classBuilder.constructorProperty("${prop.name}Behavior", "(() -> ${prop.type})?") {
                private()
                this.defaultValue = "null"
            }
        } else {
            classBuilder.constructorProperty("${prop.name}Behavior", "() -> ${prop.type}") {
                private()
                this.defaultValue = "{ $defaultExpr }"
            }
        }
    }
}

/**
 * Context for method override generation.
 *
 * Groups class-level information needed during method override generation.
 *
 * @property isClass Whether extending a class (true) vs implementing interface (false)
 * @property interfaceName Name of the interface for collision-safe data class naming
 * @property classTypeParameters Class-level type parameters (e.g., ["T", "K : Comparable<K>"])
 * @property generateCallHistory When true, includes call tracking in method body. Default: true.
 */
private data class MethodOverrideContext(
    val isClass: Boolean = false,
    val interfaceName: String = "",
    val classTypeParameters: List<String> = emptyList(),
    val generateCallHistory: Boolean = true,
)

/**
 * Generates ONLY the override method implementation. Part of Section 3: Override Implementations
 */
private fun generateMethodOverride(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    context: MethodOverrideContext,
) {
    val isOpenMethod = context.isClass && !method.isAbstract

    if (method.isVararg && method.params.size == 1) {
        val (varargName, varargType, _) = method.params.first()
        classBuilder.overrideVarargMethod(
            name = method.name,
            varargName = varargName,
            varargType = varargType,
            returnType = method.returnType,
            config =
                OverrideVarargConfig(
                    useSuperDelegation = isOpenMethod,
                    extensionReceiverType = method.extensionReceiverType,
                    isOperator = method.isOperator,
                    generateCallHistory = context.generateCallHistory,
                ),
        )
    } else {
        classBuilder.overrideMethod(
            name = method.name,
            params = method.params,
            returnType = method.returnType,
            config =
                OverrideMethodConfig(
                    isSuspend = method.isSuspend,
                    typeParameters = method.typeParameters,
                    useSuperDelegation = isOpenMethod,
                    extensionReceiverType = method.extensionReceiverType,
                    isOperator = method.isOperator,
                    interfaceName = context.interfaceName,
                    classTypeParameters = context.classTypeParameters,
                    generateCallHistory = context.generateCallHistory,
                ),
        )
    }
}

/**
 * Generates ONLY the override property implementation. Part of Section 3: Override Implementations
 *
 * Dispatches to specialized helpers based on property mutability.
 *
 * @param generateCallHistory When true, includes call count tracking in getter/setter. Default:
 *   true.
 */
private fun generatePropertyOverride(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    isClass: Boolean = false,
    generateCallHistory: Boolean = true,
) {
    val isOpenProperty = isClass && !prop.isAbstract
    if (prop.isMutable) {
        generateMutablePropertyOverride(classBuilder, prop, isOpenProperty, generateCallHistory)
    } else {
        generateImmutablePropertyOverride(classBuilder, prop, isOpenProperty, generateCallHistory)
    }
}

/**
 * Generates override for mutable (var) properties.
 *
 * Generates pattern with getter/setter:
 * - For abstract/interface: direct getter/setter invocation
 * - For open: nullable getter/setter with super delegation
 */
private fun generateMutablePropertyOverride(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
) {
    val capitalizedName = prop.name.replaceFirstChar { it.uppercase() }

    classBuilder.property(prop.name, prop.type) {
        override()
        mutable()
        getter = buildMutablePropertyGetter(prop.name, isOpenProperty, generateCallHistory)
        setter =
            buildMutablePropertySetter(
                prop.name,
                capitalizedName,
                isOpenProperty,
                generateCallHistory,
            )
    }
}

/** Builds the getter expression for a mutable property. */
private fun buildMutablePropertyGetter(
    propName: String,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
): String =
    if (isOpenProperty) {
        if (generateCallHistory) {
            listOf(
                    "_${propName}CallCount.update { it + 1 }",
                    "return ${propName}Getter?.invoke() ?: super.$propName",
                )
                .joinToString("\n")
        } else {
            "${propName}Getter?.invoke() ?: super.$propName"
        }
    } else {
        if (generateCallHistory) {
            listOf("_${propName}CallCount.update { it + 1 }", "return ${propName}Getter()")
                .joinToString("\n")
        } else {
            "${propName}Getter()"
        }
    }

/** Builds the setter expression for a mutable property. */
private fun buildMutablePropertySetter(
    propName: String,
    capitalizedName: String,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
): String =
    if (isOpenProperty) {
        if (generateCallHistory) {
            listOf(
                    "_set${capitalizedName}CallCount.update { it + 1 }",
                    "${propName}Setter?.invoke(value) ?: run { super.$propName = value }",
                )
                .joinToString("\n")
        } else {
            "${propName}Setter?.invoke(value) ?: run { super.$propName = value }"
        }
    } else {
        if (generateCallHistory) {
            listOf("_set${capitalizedName}CallCount.update { it + 1 }", "${propName}Setter(value)")
                .joinToString("\n")
        } else {
            "${propName}Setter(value)"
        }
    }

/**
 * Generates override for immutable (val) properties.
 *
 * Generates pattern with getter only:
 * - For abstract/interface: direct behavior invocation
 * - For open: nullable behavior with super delegation
 */
private fun generateImmutablePropertyOverride(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
) {
    classBuilder.property(prop.name, prop.type) {
        override()
        getter = buildImmutablePropertyGetter(prop.name, isOpenProperty, generateCallHistory)
    }
}

/** Builds the getter expression for an immutable property. */
private fun buildImmutablePropertyGetter(
    propName: String,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
): String =
    if (isOpenProperty) {
        if (generateCallHistory) {
            listOf(
                    "_${propName}CallCount.update { it + 1 }",
                    "return ${propName}Behavior?.invoke() ?: super.$propName",
                )
                .joinToString("\n")
        } else {
            "${propName}Behavior?.invoke() ?: super.$propName"
        }
    } else {
        if (generateCallHistory) {
            listOf("_${propName}CallCount.update { it + 1 }", "return ${propName}Behavior()")
                .joinToString("\n")
        } else {
            "${propName}Behavior()"
        }
    }

/**
 * Builds a function type string for behavior lambdas.
 *
 * @param paramTypes Parameter types for the lambda
 * @param returnType Return type of the lambda
 * @param isSuspend Whether the lambda is suspend
 * @param isNullable Whether the function type should be nullable (for open methods)
 * @return Function type string (e.g., "(String) -> User?" or "(suspend (String) -> User?)?")
 */
private fun buildBehaviorFunctionType(
    paramTypes: List<String>,
    returnType: String,
    isSuspend: Boolean = false,
    isNullable: Boolean = false,
): String {
    val suspendPrefix = if (isSuspend) "suspend " else ""
    val paramsStr = paramTypes.joinToString(", ")
    val baseType = "$suspendPrefix($paramsStr) -> $returnType"
    return if (isNullable) "($baseType)?" else baseType
}
