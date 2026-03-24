// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.ConstructorPropertyBuilder
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
 * @property generateMutableBehaviors When true, generates mutable behavior properties (internal
 *   var) and a configure {} method. When false, generates immutable private val behaviors. Default:
 *   false.
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
    val generateMutableBehaviors: Boolean = false,
    val superConstructorCall: String = "",
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
 * @param generateMutableBehaviors When true, generates mutable behavior properties (internal var)
 *   and a configure {} method. When false, generates immutable private val behaviors. Default:
 *   false.
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
    generateMutableBehaviors: Boolean = false,
    superConstructorCall: String = "",
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
            generateMutableBehaviors = generateMutableBehaviors,
            superConstructorCall = superConstructorCall,
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
    val generateMutableBehaviors = config.generateMutableBehaviors
    val superConstructorCall = config.superConstructorCall
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

        // Add @Volatile import for mutable behaviors
        if (generateMutableBehaviors) {
            import("kotlin.concurrent.Volatile")
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
            // Classes need constructor call: ClassName(args) or ClassName<T>(args)
            // Interfaces don't: InterfaceName or InterfaceName<T>
            val superTypeWithConstructor =
                if (isClass) "$superType($superConstructorCall)" else superType
            implements(superTypeWithConstructor)

            // Filter out StateFlow properties (they have their own tracking)
            val simpleProperties = properties.filter { !it.isStateFlow }

            // ==========================================
            // Constructor: Behavior parameters
            // When mutable: internal var — behaviors can be reassigned via configure {}
            // When immutable: private val — set at construction time via Config DSL
            // ==========================================
            simpleProperties.forEach { prop ->
                generatePropertyBehaviorConstructorParam(
                    this,
                    prop,
                    isClass,
                    generateMutableBehaviors,
                )
            }
            methods.forEach { method ->
                generateMethodBehaviorConstructorParam(
                    this,
                    method,
                    isClass,
                    generateMutableBehaviors,
                )
            }

            // ==========================================
            // SECTION 1: Interface/Class Implementation
            // ==========================================

            // Generate StateFlow property overrides (these handle tracking internally)
            properties
                .filter { it.isStateFlow }
                .forEach { prop -> generateStateFlowProperty(this, prop, resolver) }

            // Generate property overrides for simple properties
            simpleProperties.forEach { prop ->
                generatePropertyOverride(
                    this,
                    prop,
                    isClass,
                    generateCallHistory,
                    generateMutableBehaviors,
                )
            }

            // Generate method overrides
            val methodContext =
                MethodOverrideContext(
                    isClass = isClass,
                    interfaceName = interfaceName,
                    classTypeParameters = typeParameters,
                    generateCallHistory = generateCallHistory,
                    generateMutableBehaviors = generateMutableBehaviors,
                )
            methods.forEach { method -> generateMethodOverride(this, method, methodContext) }

            // ==========================================
            // SECTION 2: Call History
            // Public MutableStateFlow fields for tracking calls.
            // ==========================================
            if (generateCallHistory) {
                // Generate call history fields for properties
                simpleProperties.forEach { prop ->
                    generatePropertyCallHistoryFields(this, prop, visibility)
                }

                // Generate call history backing fields for ALL methods
                methods.forEach { method ->
                    generateMethodCallHistoryBackingField(this, method, interfaceName, visibility)
                }
            }

            // ==========================================
            // SECTION 3: Modify Method (mutable fakes only)
            // Allows selective reconfiguration of behaviors mid-test.
            // ==========================================
            if (generateMutableBehaviors) {
                generateModifyMethod(
                    classBuilder = this,
                    configClassName = "Fake${interfaceName}Config",
                    methods = methods,
                    properties = simpleProperties,
                    typeParamNames = typeParamNames,
                    visibility = visibility,
                    interfaceName = interfaceName,
                )
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
 * Generates public call history backing field for method tracking. Generated for ALL methods:
 * - Methods with params: stores data class instances
 * - 0-param/vararg-only methods: stores Unit
 */
private fun generateMethodCallHistoryBackingField(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    interfaceName: String,
    visibility: FirVisibility,
) {
    val storageInfo = resolveHistoryStorageType(interfaceName, method.name, method.params)
    classBuilder.callHistoryBackingField(method.name, storageInfo.dataClassName, visibility)
}

/** Generates public call history fields for property tracking (getter + optional setter). */
private fun generatePropertyCallHistoryFields(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    visibility: FirVisibility,
) {
    classBuilder.propertyGetterCallHistoryField(prop.name, visibility)
    if (prop.isMutable) {
        classBuilder.propertySetterCallHistoryField(prop.name, visibility)
    }
}

/**
 * Generates a constructor property for a method behavior.
 *
 * When immutable: `private val findByIdBehavior: (String) -> User?` When mutable: plain constructor
 * param + `@Volatile private var _findByIdBehavior = findByIdBehavior`
 */
private fun generateMethodBehaviorConstructorParam(
    classBuilder: ClassBuilder,
    method: MethodSpec,
    isClass: Boolean = false,
    isMutableBehavior: Boolean = false,
) {
    val isOpenMethod = isClass && !method.isAbstract
    val functionType = buildErasedBehaviorType(method = method, isOpenMethod = isOpenMethod)

    classBuilder.constructorProperty("${method.name}Behavior", functionType) {
        if (isMutableBehavior) {
            plainParam()
        } else {
            private()
        }
        // Open methods use null to signal "delegate to super"
        if (isOpenMethod) this.defaultValue = "null"
    }

    // For mutable behaviors, add @Volatile private var backing field
    if (isMutableBehavior) {
        classBuilder.property("_${method.name}Behavior", functionType) {
            annotation("Volatile")
            private()
            mutable()
            initializer = "${method.name}Behavior"
        }
    }
}

/**
 * Builds the erased function type string for a method's behavior constructor parameter.
 *
 * Erases method-level type parameters to `Any?`, handles vararg array covariance, and includes
 * extension receiver types when present.
 */
private fun buildErasedBehaviorType(method: MethodSpec, isOpenMethod: Boolean): String {
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

    return buildBehaviorFunctionType(
        paramTypes = behaviorFinalParamTypes,
        returnType = erasedReturnType,
        isSuspend = method.isSuspend,
        isNullable = isOpenMethod,
    )
}

/**
 * Generates constructor properties for a property behavior.
 *
 * When immutable: `private val` constructor properties. When mutable: plain constructor params +
 * `@Volatile private var` backing fields.
 */
private fun generatePropertyBehaviorConstructorParam(
    classBuilder: ClassBuilder,
    prop: PropertySpec,
    isClass: Boolean = false,
    isMutableBehavior: Boolean = false,
) {
    val isOpenProperty = isClass && !prop.isAbstract

    fun ConstructorPropertyBuilder.applyVisibility() {
        if (isMutableBehavior) {
            plainParam()
        } else {
            private()
        }
    }

    fun ClassBuilder.addVolatileBackingField(name: String, type: String) {
        if (isMutableBehavior) {
            property("_$name", type) {
                annotation("Volatile")
                private()
                mutable()
                initializer = name
            }
        }
    }

    if (prop.isMutable) {
        if (isOpenProperty) {
            classBuilder.constructorProperty("${prop.name}Getter", "(() -> ${prop.type})?") {
                applyVisibility()
                this.defaultValue = "null"
            }
            classBuilder.constructorProperty("${prop.name}Setter", "((${prop.type}) -> Unit)?") {
                applyVisibility()
                this.defaultValue = "null"
            }
        } else {
            classBuilder.constructorProperty("${prop.name}Getter", "() -> ${prop.type}") {
                applyVisibility()
            }
            classBuilder.constructorProperty("${prop.name}Setter", "(${prop.type}) -> Unit") {
                applyVisibility()
            }
        }
        if (isMutableBehavior) {
            classBuilder.addVolatileBackingField(
                "${prop.name}Getter",
                if (isOpenProperty) "(() -> ${prop.type})?" else "() -> ${prop.type}",
            )
            classBuilder.addVolatileBackingField(
                "${prop.name}Setter",
                if (isOpenProperty) "((${prop.type}) -> Unit)?" else "(${prop.type}) -> Unit",
            )
        }
    } else {
        if (isOpenProperty) {
            classBuilder.constructorProperty("${prop.name}Behavior", "(() -> ${prop.type})?") {
                applyVisibility()
                this.defaultValue = "null"
            }
        } else {
            classBuilder.constructorProperty("${prop.name}Behavior", "() -> ${prop.type}") {
                applyVisibility()
            }
        }
        classBuilder.addVolatileBackingField(
            "${prop.name}Behavior",
            if (isOpenProperty) "(() -> ${prop.type})?" else "() -> ${prop.type}",
        )
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
    val generateMutableBehaviors: Boolean = false,
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
    val behaviorPrefix = if (context.generateMutableBehaviors) "_" else ""

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
                    behaviorPrefix = behaviorPrefix,
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
                    behaviorPrefix = behaviorPrefix,
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
    generateMutableBehaviors: Boolean = false,
) {
    val isOpenProperty = isClass && !prop.isAbstract
    val behaviorPrefix = if (generateMutableBehaviors) "_" else ""
    if (prop.isMutable) {
        generateMutablePropertyOverride(
            classBuilder,
            prop,
            isOpenProperty,
            generateCallHistory,
            behaviorPrefix,
        )
    } else {
        generateImmutablePropertyOverride(
            classBuilder,
            prop,
            isOpenProperty,
            generateCallHistory,
            behaviorPrefix,
        )
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
    behaviorPrefix: String = "",
) {
    val capitalizedName = prop.name.replaceFirstChar { it.uppercase() }

    classBuilder.property(prop.name, prop.type) {
        override()
        mutable()
        getter =
            buildMutablePropertyGetter(
                prop.name,
                isOpenProperty,
                generateCallHistory,
                behaviorPrefix,
            )
        setter =
            buildMutablePropertySetter(
                prop.name,
                capitalizedName,
                isOpenProperty,
                generateCallHistory,
                behaviorPrefix,
            )
    }
}

/** Builds the getter expression for a mutable property. */
private fun buildMutablePropertyGetter(
    propName: String,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
    behaviorPrefix: String = "",
): String =
    if (isOpenProperty) {
        if (generateCallHistory) {
            listOf(
                    "${propName}Calls.update { it + Unit }",
                    "return $behaviorPrefix${propName}Getter?.invoke() ?: super.$propName",
                )
                .joinToString("\n")
        } else {
            "$behaviorPrefix${propName}Getter?.invoke() ?: super.$propName"
        }
    } else {
        if (generateCallHistory) {
            listOf(
                    "${propName}Calls.update { it + Unit }",
                    "return $behaviorPrefix${propName}Getter()",
                )
                .joinToString("\n")
        } else {
            "$behaviorPrefix${propName}Getter()"
        }
    }

/** Builds the setter expression for a mutable property. */
private fun buildMutablePropertySetter(
    propName: String,
    capitalizedName: String,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
    behaviorPrefix: String = "",
): String =
    if (isOpenProperty) {
        if (generateCallHistory) {
            listOf(
                    "set${capitalizedName}Calls.update { it + Unit }",
                    "$behaviorPrefix${propName}Setter?.invoke(value) ?: run { super.$propName = value }",
                )
                .joinToString("\n")
        } else {
            "$behaviorPrefix${propName}Setter?.invoke(value) ?: run { super.$propName = value }"
        }
    } else {
        if (generateCallHistory) {
            listOf(
                    "set${capitalizedName}Calls.update { it + Unit }",
                    "$behaviorPrefix${propName}Setter(value)",
                )
                .joinToString("\n")
        } else {
            "$behaviorPrefix${propName}Setter(value)"
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
    behaviorPrefix: String = "",
) {
    classBuilder.property(prop.name, prop.type) {
        override()
        getter =
            buildImmutablePropertyGetter(
                prop.name,
                isOpenProperty,
                generateCallHistory,
                behaviorPrefix,
            )
    }
}

/** Builds the getter expression for an immutable property. */
private fun buildImmutablePropertyGetter(
    propName: String,
    isOpenProperty: Boolean,
    generateCallHistory: Boolean,
    behaviorPrefix: String = "",
): String =
    if (isOpenProperty) {
        if (generateCallHistory) {
            listOf(
                    "${propName}Calls.update { it + Unit }",
                    "return $behaviorPrefix${propName}Behavior?.invoke() ?: super.$propName",
                )
                .joinToString("\n")
        } else {
            "$behaviorPrefix${propName}Behavior?.invoke() ?: super.$propName"
        }
    } else {
        if (generateCallHistory) {
            listOf(
                    "${propName}Calls.update { it + Unit }",
                    "return $behaviorPrefix${propName}Behavior()",
                )
                .joinToString("\n")
        } else {
            "$behaviorPrefix${propName}Behavior()"
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

/**
 * Generates the `modify {}` method on a mutable fake implementation.
 *
 * The modify method:
 * 1. Creates a fresh Config instance (with null behavior defaults)
 * 2. Applies the user's DSL block
 * 3. Selectively updates only behaviors that were explicitly set (non-null)
 *
 * This allows partial reconfiguration:
 * ```kotlin
 * fake.modify {
 *     findById { null }  // Only changes this, other behaviors unchanged
 * }
 * ```
 */
private fun generateModifyMethod(
    classBuilder: ClassBuilder,
    configClassName: String,
    methods: List<MethodSpec>,
    properties: List<PropertySpec>,
    typeParamNames: List<String>,
    visibility: FirVisibility,
    interfaceName: String,
) {
    val configTypeArgs =
        if (typeParamNames.isNotEmpty()) "<${typeParamNames.joinToString(", ")}>" else ""

    val bodyLines = mutableListOf<String>()
    bodyLines.add("val config = $configClassName$configTypeArgs().apply(block)")

    // Generate selective updates for method behaviors (using _ prefix for backing fields)
    methods.forEach { method ->
        bodyLines.add("config.${method.name}Behavior?.let { _${method.name}Behavior = it }")
    }

    // Generate selective updates for property behaviors (using _ prefix for backing fields)
    properties.forEach { prop ->
        if (prop.isMutable) {
            bodyLines.add("config.${prop.name}Getter?.let { _${prop.name}Getter = it }")
            bodyLines.add(
                "config.set${prop.name.replaceFirstChar { it.uppercase() }}Behavior?.let { _${prop.name}Setter = it }"
            )
        } else {
            bodyLines.add("config.${prop.name}Behavior?.let { _${prop.name}Behavior = it }")
        }
    }

    val kdoc = buildModifyKDoc(interfaceName, methods, properties)

    classBuilder.function("modify") {
        this.kdoc = kdoc
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE,
            FirVisibility.PROTECTED -> public()
        }
        parameter("block", "$configClassName$configTypeArgs.() -> Unit")
        body = bodyLines.joinToString("\n")
    }
}

/** Builds contextual KDoc for the `modify` method showing actual behaviors. */
private fun buildModifyKDoc(
    interfaceName: String,
    methods: List<MethodSpec>,
    properties: List<PropertySpec>,
): String = buildString {
    appendLine("Selectively modifies fake behaviors.")
    appendLine()
    appendLine("Only behaviors specified in [block] are updated; all others remain unchanged.")
    appendLine()
    appendLine("Thread-safe: backing fields use @Volatile for cross-thread visibility.")
    appendLine()

    // Generate contextual example showing first behavior
    appendLine("```kotlin")
    appendLine("fake.modify {")
    val firstBehavior = methods.firstOrNull()
    val firstProperty = properties.firstOrNull()
    when {
        firstBehavior != null -> appendLine("    ${firstBehavior.name} { /* new behavior */ }")
        firstProperty != null -> appendLine("    ${firstProperty.name} { /* new behavior */ }")
    }
    appendLine("}")
    appendLine("```")

    // List modifiable behaviors
    if (methods.isNotEmpty() || properties.isNotEmpty()) {
        appendLine()
        appendLine("## Modifiable Behaviors")
        appendLine()
        methods.forEach { method ->
            val params = method.params.joinToString(", ") { (name, type, _) -> "$name: $type" }
            val suspend = if (method.isSuspend) " (suspend)" else ""
            appendLine("- `${method.name}`: ($params) -> ${method.returnType}$suspend")
        }
        properties.forEach { prop -> appendLine("- `${prop.name}`: ${prop.type}") }
    }
    appendLine()
    append("@see Fake${interfaceName}Config")
}
