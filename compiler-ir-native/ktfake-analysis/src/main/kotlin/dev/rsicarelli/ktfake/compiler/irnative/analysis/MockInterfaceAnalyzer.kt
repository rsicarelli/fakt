// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.irnative.analysis

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.IrClass

/**
 * Mock implementation of InterfaceAnalyzer for demonstrating IR-Native capabilities.
 *
 * This implementation simulates complete interface analysis using static data,
 * allowing us to test the entire IR-Native pipeline without complex IR APIs.
 *
 * Once IR API integration is complete, this will be replaced by IrInterfaceAnalyzer.
 */
class MockInterfaceAnalyzer : InterfaceAnalyzer {

    /**
     * Create comprehensive analysis for any interface.
     * Simulates what IrInterfaceAnalyzer would extract from real IR.
     */
    override fun analyzeInterface(sourceInterface: IrClass): InterfaceAnalysis {
        val interfaceName = sourceInterface.name.asString()

        // Generate realistic method analysis based on common interface patterns
        val methods = generateMockMethods(interfaceName)
        val properties = generateMockProperties(interfaceName)
        val generics = generateMockGenerics(interfaceName)

        return InterfaceAnalysis(
            sourceInterface = sourceInterface,
            interfaceName = interfaceName,
            packageName = extractPackageName(interfaceName),
            methods = methods,
            properties = properties,
            generics = generics,
            annotations = AnnotationAnalysis(
                trackCalls = false,
                builder = false,
                concurrent = true,
                scope = "test",
                dependencies = emptyList()
            ),
            dependencies = emptyList()
        )
    }

    /**
     * Discover fake interfaces - simplified for demonstration.
     */
    override fun discoverFakeInterfaces(moduleClasses: List<IrClass>): List<IrClass> {
        return moduleClasses.filter { irClass ->
            irClass.kind == ClassKind.INTERFACE &&
            irClass.annotations.isNotEmpty()
        }
    }

    /**
     * Basic validation implementation.
     */
    override fun validateInterface(sourceInterface: IrClass): ValidationResult {
        val errors = mutableListOf<String>()

        if (sourceInterface.kind == ClassKind.OBJECT) {
            errors.add("@Fake cannot be applied to objects. Use interface or class for thread safety.")
        }

        if (sourceInterface.declarations.isEmpty()) {
            errors.add("Interface '${sourceInterface.name}' has no methods or properties to fake")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    /**
     * Generate realistic method signatures for common service patterns.
     */
    private fun generateMockMethods(interfaceName: String): List<MethodAnalysis> {
        return when {
            interfaceName.contains("User") -> listOf(
                createMethod("getUser", "User", listOf("id" to "kotlin.String")),
                createMethod("createUser", "User", listOf("name" to "kotlin.String", "email" to "kotlin.String")),
                createMethod("deleteUser", "kotlin.Unit", listOf("id" to "kotlin.String")),
                createSuspendMethod("fetchUserAsync", "User", listOf("id" to "kotlin.String"))
            )
            interfaceName.contains("Payment") -> listOf(
                createMethod("processPayment", "kotlin.Result", listOf("amount" to "kotlin.Double")),
                createMethod("getPaymentHistory", "kotlin.collections.List", listOf("userId" to "kotlin.String")),
                createSuspendMethod("validateCard", "kotlin.Boolean", listOf("cardNumber" to "kotlin.String"))
            )
            interfaceName.contains("Repository") -> listOf(
                createMethod("save", "kotlin.Unit", listOf("entity" to "T")),
                createMethod("findById", "T", listOf("id" to "kotlin.String")),
                createMethod("findAll", "kotlin.collections.List", emptyList()),
                createMethod("delete", "kotlin.Unit", listOf("id" to "kotlin.String"))
            )
            else -> listOf(
                createMethod("getData", "kotlin.String", emptyList()),
                createMethod("setData", "kotlin.Unit", listOf("data" to "kotlin.String"))
            )
        }
    }

    /**
     * Generate realistic property signatures.
     */
    private fun generateMockProperties(interfaceName: String): List<PropertyAnalysis> {
        return when {
            interfaceName.contains("Config") -> listOf(
                createProperty("apiUrl", "kotlin.String", hasGetter = true, hasSetter = false),
                createProperty("timeout", "kotlin.Int", hasGetter = true, hasSetter = true),
                createProperty("isEnabled", "kotlin.Boolean", hasGetter = true, hasSetter = false)
            )
            interfaceName.contains("Service") -> listOf(
                createProperty("name", "kotlin.String", hasGetter = true, hasSetter = false)
            )
            else -> emptyList()
        }
    }

    /**
     * Generate generic parameters for repository-like interfaces.
     */
    private fun generateMockGenerics(interfaceName: String): List<GenericAnalysis> {
        return if (interfaceName.contains("Repository")) {
            listOf(
                GenericAnalysis(
                    name = "T",
                    bounds = listOf(
                        TypeAnalysis(
                            qualifiedName = "kotlin.Any",
                            isNullable = false,
                            genericArguments = emptyList(),
                            isBuiltin = true
                        )
                    ),
                    variance = GenericVariance.INVARIANT
                )
            )
        } else {
            emptyList()
        }
    }

    /**
     * Create method analysis with realistic type information.
     */
    private fun createMethod(
        name: String,
        returnType: String,
        parameters: List<Pair<String, String>>,
        isSuspend: Boolean = false
    ): MethodAnalysis {
        val paramAnalysis = parameters.map { (paramName, paramType) ->
            ParameterAnalysis(
                name = paramName,
                type = TypeAnalysis(
                    qualifiedName = paramType,
                    isNullable = false,
                    genericArguments = emptyList(),
                    isBuiltin = paramType.startsWith("kotlin.")
                ),
                hasDefaultValue = false,
                isVararg = false
            )
        }

        val modifiers = mutableSetOf<MethodModifier>().apply {
            add(MethodModifier.ABSTRACT)
            if (isSuspend) add(MethodModifier.SUSPEND)
        }

        return MethodAnalysis(
            function = null as Any?, // Would be IrSimpleFunction in real implementation
            name = name,
            parameters = paramAnalysis,
            returnType = TypeAnalysis(
                qualifiedName = returnType,
                isNullable = false,
                genericArguments = when (returnType) {
                    "kotlin.collections.List" -> listOf(
                        TypeAnalysis("T", false, emptyList(), false)
                    )
                    "kotlin.Result" -> listOf(
                        TypeAnalysis("kotlin.Unit", false, emptyList(), true)
                    )
                    else -> emptyList()
                },
                isBuiltin = returnType.startsWith("kotlin.")
            ),
            isSuspend = isSuspend,
            modifiers = modifiers
        )
    }

    /**
     * Create suspend method variant.
     */
    private fun createSuspendMethod(name: String, returnType: String, parameters: List<Pair<String, String>>): MethodAnalysis {
        return createMethod(name, returnType, parameters, isSuspend = true)
    }

    /**
     * Create property analysis.
     */
    private fun createProperty(name: String, type: String, hasGetter: Boolean, hasSetter: Boolean): PropertyAnalysis {
        return PropertyAnalysis(
            property = null as Any?, // Would be IrProperty in real implementation
            name = name,
            type = TypeAnalysis(
                qualifiedName = type,
                isNullable = false,
                genericArguments = emptyList(),
                isBuiltin = type.startsWith("kotlin.")
            ),
            hasGetter = hasGetter,
            hasSetter = hasSetter,
            modifiers = setOf(PropertyModifier.ABSTRACT)
        )
    }

    /**
     * Extract package name from interface name (simplified).
     */
    private fun extractPackageName(interfaceName: String): String {
        return when {
            interfaceName.contains("User") -> "com.example.user"
            interfaceName.contains("Payment") -> "com.example.payment"
            interfaceName.contains("Repository") -> "com.example.repository"
            else -> "com.example"
        }
    }
}
